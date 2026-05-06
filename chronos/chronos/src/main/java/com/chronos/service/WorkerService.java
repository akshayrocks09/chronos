package com.chronos.service;

import com.chronos.entity.Job;
import com.chronos.entity.JobExecutionLog;
import com.chronos.enums.JobStatus;
import com.chronos.enums.JobType;
import com.chronos.exception.RetryableJobException;
import com.chronos.repository.JobExecutionLogRepository;
import com.chronos.repository.JobRepository;
import com.chronos.scheduler.JobSchedulerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;

/**
 * WorkerService — responsible for executing a single job invocation.
 *
 * Transaction design (critical for correctness at scale):
 *   - executeJob()   runs in its OWN new transaction so success/failure
 *                    state is always committed independently of the caller.
 *   - handleFailure() runs in a SEPARATE new transaction so that the
 *                    rollback of the main execution never undoes the
 *                    failure-state update or the retry schedule.
 *
 * This prevents the core bug where @Transactional + throw ex caused
 * all failure-state saves (retryCount, RETRYING status, execLog) to
 * be silently rolled back.
 */
@Service
public class WorkerService {

    private static final Logger log = LoggerFactory.getLogger(WorkerService.class);

    private final JobRepository jobRepository;
    private final JobExecutionLogRepository logRepository;
    private final NotificationService notificationService;
    private final JobSchedulerService schedulerService;
    private final org.springframework.beans.factory.ObjectProvider<WorkerService> selfProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    @Value("${chronos.job.retry-delay-seconds:10}")
    private long retryDelaySeconds;

    public WorkerService(JobRepository jobRepository,
                         JobExecutionLogRepository logRepository,
                         NotificationService notificationService,
                         JobSchedulerService schedulerService,
                         org.springframework.beans.factory.ObjectProvider<WorkerService> selfProvider) {
        this.jobRepository = jobRepository;
        this.logRepository = logRepository;
        this.notificationService = notificationService;
        this.schedulerService = schedulerService;
        this.selfProvider = selfProvider;
        this.restTemplate = buildRestTemplate();
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        return new RestTemplate(factory);
    }

    /**
     * Entry point called by ChronosQuartzJob.
     * Runs in REQUIRES_NEW so it always commits (or rolls back) independently.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OffsetDateTime executeJob(Long jobId) throws Exception {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        // Guard: skip if already cancelled or completed
        if (job.getStatus() == JobStatus.CANCELLED) {
            log.info("[JOB_SKIPPED] Job {} is cancelled — skipping.", jobId);
            return null;
        }
        if (job.getStatus() == JobStatus.COMPLETED) {
            log.info("[JOB_SKIPPED] Job {} is already completed — skipping duplicate trigger.", jobId);
            return null;
        }

        log.info("[JOB_STARTED] Executing job '{}' (id={})", job.getName(), jobId);

        int attemptNumber = job.getRetryCount() + 1;
        long startTime = System.currentTimeMillis();
        OffsetDateTime executedAt = OffsetDateTime.now();

        // Mark RUNNING — this is in the current transaction
        job.setStatus(JobStatus.RUNNING);
        job.setLastExecutedAt(executedAt);
        jobRepository.save(job);

        try {
            process(job);

            // SUCCESS — commit log and status
            long duration = System.currentTimeMillis() - startTime;

            JobExecutionLog execLog = JobExecutionLog.builder()
                    .job(job)
                    .status(JobStatus.COMPLETED)
                    .attemptNumber(attemptNumber)
                    .executedAt(executedAt)
                    .completedAt(OffsetDateTime.now())
                    .durationMs(duration)
                    .build();
            logRepository.save(execLog);

            if (job.getJobType() == JobType.RECURRING) {
                job.setStatus(JobStatus.SCHEDULED);
                job.setNextFireAt(schedulerService.getNextFireTime(job));
            } else {
                job.setStatus(JobStatus.COMPLETED);
                job.setNextFireAt(null);
                job.setCompletedAt(OffsetDateTime.now());
            }
            job.setLastErrorMessage(null);
            jobRepository.save(job);

            log.info("[JOB_SUCCESS] Job {} completed in {}ms", jobId, duration);
            return null; // no retry needed

        } catch (Exception ex) {
            // recordFailureInNewTransaction runs in its OWN transaction.
            // It creates a fresh log entry (the current transaction's log will roll back).
            // It returns the next retry time, or null if permanently failed.
            OffsetDateTime retryAt = selfProvider.getIfAvailable()
                    .recordFailureInNewTransaction(jobId, attemptNumber, startTime, executedAt, ex.getMessage());

            // Rethrow wrapped so ChronosQuartzJob knows to schedule the retry.
            throw new RetryableJobException(retryAt, ex);
        }
    }

    /**
     * Saves failure state in its own REQUIRES_NEW transaction (always commits).
     * Creates a fresh log entry — does NOT reference the rolled-back one from executeJob.
     * Returns the next retry OffsetDateTime, or null if permanently failed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OffsetDateTime recordFailureInNewTransaction(Long jobId, int attemptNumber,
                                                       long startTime, OffsetDateTime executedAt,
                                                       String errorMessage) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found during failure recording: " + jobId));

        long duration = System.currentTimeMillis() - startTime;
        log.error("[JOB_FAILED] Job '{}' (id={}) failed on attempt {}: {}",
                job.getName(), jobId, attemptNumber, errorMessage);

        // Create a fresh FAILED log entry (the one from executeJob was rolled back)
        JobExecutionLog failLog = JobExecutionLog.builder()
                .job(job)
                .status(JobStatus.FAILED)
                .attemptNumber(attemptNumber)
                .executedAt(executedAt)
                .completedAt(OffsetDateTime.now())
                .durationMs(duration)
                .errorMessage(errorMessage)
                .build();
        logRepository.save(failLog);

        job.setRetryCount(job.getRetryCount() + 1);
        job.setLastErrorMessage(errorMessage);

        if (job.getRetryCount() < job.getMaxRetries()) {
            long delaySeconds = retryDelaySeconds * (1L << (job.getRetryCount() - 1)); // 10s, 20s, 40s…
            OffsetDateTime nextRetry = OffsetDateTime.now().plusSeconds(delaySeconds);

            job.setStatus(JobStatus.RETRYING);
            job.setNextFireAt(nextRetry);
            jobRepository.save(job);

            log.info("[RETRY_PENDING] Job {} scheduled for retry at {} (delay={}s)", jobId, nextRetry, delaySeconds);
            return nextRetry;
        } else {
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
            log.warn("[JOB_PERMANENTLY_FAILED] Job {} exhausted all {} retries", jobId, job.getMaxRetries());
            notificationService.notifyJobPermanentlyFailed(job);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Payload dispatch
    // -------------------------------------------------------------------------

    private void process(Job job) throws Exception {
        String payload = job.getPayload();

        if (payload == null || payload.isBlank()) {
            log.info("[JOB_EXEC] Job {} has no payload — nothing to do.", job.getId());
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception e) {
            throw new Exception("Payload is not valid JSON: " + e.getMessage());
        }

        // Support {"fail":true} for testing/simulation
        if (root.path("fail").asBoolean(false)) {
            throw new Exception("Simulated failure (fail=true in payload)");
        }

        String type = root.path("type").asText("LOG_MESSAGE").toUpperCase();

        switch (type) {
            case "HTTP_REQUEST" -> handleHttpRequest(job, root);
            case "SEND_EMAIL"   -> handleSendEmail(job, root);
            case "LOG_MESSAGE"  -> {
                String message = root.path("message").asText("(no message field)");
                log.info("[JOB_EXEC] Job {} LOG_MESSAGE: {}", job.getId(), message);
            }
            default -> log.info("[JOB_EXEC] Job {} unrecognised type '{}' — raw payload logged.", job.getId(), type);
        }
    }

    private void handleHttpRequest(Job job, JsonNode root) throws Exception {
        String url = root.path("url").asText(null);
        if (url == null || url.isBlank()) {
            throw new Exception("HTTP_REQUEST payload missing required field: url");
        }

        String method = root.path("method").asText("GET").toUpperCase();
        String body   = root.path("body").isMissingNode() ? null : root.path("body").toString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("User-Agent", "Chronos-JobScheduler/1.0");

        JsonNode extraHeaders = root.path("headers");
        if (!extraHeaders.isMissingNode() && extraHeaders.isObject()) {
            extraHeaders.fields().forEachRemaining(e -> headers.set(e.getKey(), e.getValue().asText()));
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.valueOf(method), entity, String.class);

        int statusCode = response.getStatusCode().value();
        if (statusCode < 200 || statusCode >= 300) {
            throw new Exception("HTTP_REQUEST to " + url + " returned non-2xx status: " + statusCode);
        }
        log.info("[JOB_EXEC] Job {} HTTP_REQUEST {} {} -> {}", job.getId(), method, url, statusCode);
    }

    private void handleSendEmail(Job job, JsonNode root) throws Exception {
        String to      = root.path("to").asText(null);
        String subject = root.path("subject").asText("Chronos notification");
        String body    = root.path("body").asText("(no body provided)");

        if (to == null || to.isBlank()) {
            throw new Exception("SEND_EMAIL payload missing required field: to");
        }

        notificationService.sendCustomEmail(to, subject, body);
        log.info("[JOB_EXEC] Job {} SEND_EMAIL -> {}", job.getId(), to);
    }
}
