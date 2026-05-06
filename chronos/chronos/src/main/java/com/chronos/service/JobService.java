package com.chronos.service;

import com.chronos.dto.JobDto;
import com.chronos.entity.Job;
import com.chronos.entity.JobExecutionLog;
import com.chronos.entity.User;
import com.chronos.enums.JobStatus;
import com.chronos.enums.JobType;
import com.chronos.enums.UserRole;
import com.chronos.exception.ResourceNotFoundException;
import com.chronos.repository.JobExecutionLogRepository;
import com.chronos.repository.JobRepository;
import com.chronos.repository.UserRepository;
import com.chronos.scheduler.JobSchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final JobExecutionLogRepository logRepository;
    private final UserRepository userRepository;
    private final JobSchedulerService schedulerService;

    @Value("${chronos.jobs.max-per-user:50}")
    private int maxJobsPerUser;

    @Value("${chronos.jobs.trigger-cooldown-seconds:30}")
    private int triggerCooldownSeconds;

    public JobService(JobRepository jobRepository,
                      JobExecutionLogRepository logRepository,
                      UserRepository userRepository,
                      JobSchedulerService schedulerService) {
        this.jobRepository = jobRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
        this.schedulerService = schedulerService;
    }

    // ------------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------------

    @Transactional
    public JobDto.JobResponse createJob(String username, JobDto.CreateJobRequest request) {
        User user = getUser(username);

        // Rate Limit Check
        long activeJobs = jobRepository.countByUserIdAndStatusIn(user.getId(), 
            List.of(JobStatus.PENDING, JobStatus.SCHEDULED, JobStatus.RUNNING, JobStatus.RETRYING));
        
        if (activeJobs >= maxJobsPerUser) {
            throw new IllegalStateException("User has reached the maximum limit of " + maxJobsPerUser + " active jobs.");
        }

        if (request.getJobType() == JobType.RECURRING) {
            if (request.getCronExpression() == null || request.getCronExpression().isBlank()) {
                throw new IllegalArgumentException("cronExpression is required for RECURRING jobs");
            }
        }

        Job job = Job.builder()
                .name(request.getName())
                .description(request.getDescription())
                .jobType(request.getJobType())
                .status(JobStatus.PENDING)
                .cronExpression(request.getCronExpression())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .scheduledAt(request.getScheduledAt())
                .payload(request.getPayload())
                .maxRetries(request.getMaxRetries())
                .retryCount(0)
                .user(user)
                .build();

        job = jobRepository.save(job);
        job.setQuartzJobKey("job-" + job.getId());

        if (job.getJobType() == JobType.ONE_TIME) {
            job.setStatus(JobStatus.PENDING);
        } else {
            job.setStatus(JobStatus.SCHEDULED);
        }
        job.setNextFireAt(schedulerService.calculateNextFireTime(job));
        job = jobRepository.save(job);

        final Job finalJob = job;
        registerAfterCommit(() -> schedulerService.scheduleJob(finalJob));

        log.info("Created job id={} name='{}' for user={}", job.getId(), job.getName(), username);
        return toResponse(job);
    }

    // ------------------------------------------------------------------
    // READ (USER SCOPE)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<JobDto.JobResponse> getUserJobs(String username, JobStatus status, String search, Pageable pageable) {
        User user = getUser(username);
        String s = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Job> jobs;

        if (status != null && s != null)      jobs = jobRepository.findByUserIdAndStatusAndNameContainingIgnoreCase(user.getId(), status, s, pageable);
        else if (status != null)              jobs = jobRepository.findByUserIdAndStatus(user.getId(), status, pageable);
        else if (s != null)                   jobs = jobRepository.findByUserIdAndNameContainingIgnoreCase(user.getId(), s, pageable);
        else                                  jobs = jobRepository.findByUserId(user.getId(), pageable);

        return jobs.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public JobDto.JobResponse getJobById(String username, Long jobId) {
        User user = getUser(username);
        Job job = jobRepository.findByIdAndUserId(jobId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public Page<JobExecutionLog> getJobLogs(String username, Long jobId, int size) {
        User user = getUser(username);
        // Security check: ensure job belongs to user
        jobRepository.findByIdAndUserId(jobId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        
        return logRepository.findByJobId(jobId, PageRequest.of(0, size, Sort.by("executedAt").descending()));
    }

    @Transactional(readOnly = true)
    public Page<JobDto.LogResponse> getUserLogs(String username, JobStatus status, Pageable pageable) {
        User user = getUser(username);
        Page<JobExecutionLog> logs = (status != null)
                ? logRepository.findByJobUserIdAndStatusOrderByExecutedAtDesc(user.getId(), status, pageable)
                : logRepository.findByJobUserIdOrderByExecutedAtDesc(user.getId(), pageable);
        return logs.map(this::toLogResponse);
    }

    @Transactional(readOnly = true)
    public JobDto.JobStatsResponse getUserStats(String username) {
        User user = getUser(username);
        return JobDto.JobStatsResponse.builder()
                .totalJobs(jobRepository.countByUserId(user.getId()))
                .pendingJobs(jobRepository.countByUserIdAndStatus(user.getId(), JobStatus.PENDING))
                .runningJobs(jobRepository.countByUserIdAndStatus(user.getId(), JobStatus.RUNNING))
                .completedJobs(jobRepository.countByUserIdAndStatus(user.getId(), JobStatus.COMPLETED))
                .failedJobs(jobRepository.countByUserIdAndStatus(user.getId(), JobStatus.FAILED))
                .cancelledJobs(jobRepository.countByUserIdAndStatus(user.getId(), JobStatus.CANCELLED))
                .scheduledJobs(jobRepository.countByUserIdAndStatus(user.getId(), JobStatus.SCHEDULED))
                .build();
    }

    // ------------------------------------------------------------------
    // READ (ADMIN SCOPE)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<JobDto.JobResponse> getAllSystemJobs(JobStatus status, String search, Pageable pageable) {
        String s = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Job> jobs;

        if (status != null && s != null)      jobs = jobRepository.findByStatusAndNameContainingIgnoreCase(status, s, pageable);
        else if (status != null)              jobs = jobRepository.findByStatus(status, pageable);
        else if (s != null)                   jobs = jobRepository.findByNameContainingIgnoreCase(s, pageable);
        else                                  jobs = jobRepository.findAll(pageable);

        return jobs.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public JobDto.JobResponse getAnyJobById(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public Page<JobDto.LogResponse> getAllSystemLogs(JobStatus status, Pageable pageable) {
        Page<JobExecutionLog> logs = (status != null)
                ? logRepository.findByStatusOrderByExecutedAtDesc(status, pageable)
                : logRepository.findAllByOrderByExecutedAtDesc(pageable);
        return logs.map(this::toLogResponse);
    }

    @Transactional(readOnly = true)
    public JobDto.JobStatsResponse getSystemStats() {
        return JobDto.JobStatsResponse.builder()
                .totalJobs(jobRepository.count())
                .pendingJobs(jobRepository.countByStatus(JobStatus.PENDING))
                .runningJobs(jobRepository.countByStatus(JobStatus.RUNNING))
                .completedJobs(jobRepository.countByStatus(JobStatus.COMPLETED))
                .failedJobs(jobRepository.countByStatus(JobStatus.FAILED))
                .cancelledJobs(jobRepository.countByStatus(JobStatus.CANCELLED))
                .scheduledJobs(jobRepository.countByStatus(JobStatus.SCHEDULED))
                .build();
    }

    // ------------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------------

    @Transactional
    public JobDto.JobResponse updateJob(String username, Long jobId, JobDto.UpdateJobRequest request) {
        User user = getUser(username);
        Job job = jobRepository.findByIdAndUserId(jobId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (job.getStatus() == JobStatus.RETRYING) {
            throw new IllegalStateException("Cannot update a job that is currently retrying. Cancel it first.");
        }
        if (job.getStatus() == JobStatus.RUNNING) {
            throw new IllegalStateException("Cannot update a job that is currently running");
        }

        if (request.getName() != null)           job.setName(request.getName());
        if (request.getDescription() != null)    job.setDescription(request.getDescription());
        if (request.getCronExpression() != null) job.setCronExpression(request.getCronExpression());
        if (request.getTimezone() != null)       job.setTimezone(request.getTimezone());
        if (request.getScheduledAt() != null)    job.setScheduledAt(request.getScheduledAt());
        if (request.getPayload() != null)        job.setPayload(request.getPayload());
        if (request.getMaxRetries() != null)     job.setMaxRetries(request.getMaxRetries());

        if (job.getStatus() == JobStatus.CANCELLED) {
            job.setStatus(job.getJobType() == JobType.ONE_TIME ? JobStatus.PENDING : JobStatus.SCHEDULED);
        }

        job.setNextFireAt(schedulerService.calculateNextFireTime(job));
        job = jobRepository.save(job);

        final Job finalJob = job;
        registerAfterCommit(() -> schedulerService.rescheduleJob(finalJob));

        log.info("Updated job id={}", jobId);
        return toResponse(job);
    }

    // ------------------------------------------------------------------
    // CANCEL / DELETE
    // ------------------------------------------------------------------

    @Transactional
    public JobDto.JobResponse cancelJob(String username, Long jobId, boolean isAdmin) {
        Job job = isAdmin 
                ? jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"))
                : jobRepository.findByIdAndUserId(jobId, getUser(username).getId()).orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (job.getStatus() == JobStatus.CANCELLED) throw new IllegalStateException("Job is already cancelled");
        if (job.getStatus() == JobStatus.COMPLETED)  throw new IllegalStateException("Cannot cancel a completed job");

        schedulerService.cancelJob(job);
        job.setStatus(JobStatus.CANCELLED);
        job = jobRepository.save(job);

        log.info("Cancelled job id={} by user={}", jobId, username);
        return toResponse(job);
    }

    @Transactional
    public void deleteJob(String username, Long jobId, boolean isAdmin) {
        Job job = isAdmin 
                ? jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"))
                : jobRepository.findByIdAndUserId(jobId, getUser(username).getId()).orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        try {
            schedulerService.cancelJob(job);
        } catch (Exception e) {
            log.warn("Failed to cancel Quartz job {} during deletion: {}", jobId, e.getMessage());
        }

        jobRepository.delete(job);
        log.info("Job {} deleted by user {}", jobId, username);
    }

    // ------------------------------------------------------------------
    // MANUAL TRIGGER
    // ------------------------------------------------------------------

    @Transactional
    public JobDto.JobResponse triggerJobNow(String username, Long jobId, boolean isAdmin) {
        Job job = isAdmin 
                ? jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"))
                : jobRepository.findByIdAndUserId(jobId, getUser(username).getId()).orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (job.getLastTriggeredAt() != null) {
            OffsetDateTime nextAllowed = job.getLastTriggeredAt().plusSeconds(triggerCooldownSeconds);
            if (OffsetDateTime.now().isBefore(nextAllowed)) {
                long wait = Duration.between(OffsetDateTime.now(), nextAllowed).getSeconds();
                throw new IllegalStateException("Job triggered too recently. Please wait " + wait + " more seconds.");
            }
        }

        if (job.getStatus() == JobStatus.CANCELLED) throw new IllegalStateException("Cannot trigger a cancelled job");
        if (job.getStatus() == JobStatus.COMPLETED)  throw new IllegalStateException("Cannot re-trigger a completed one-time job.");
        if (job.getStatus() == JobStatus.RUNNING)    throw new IllegalStateException("Job is already running");

        job.setLastTriggeredAt(OffsetDateTime.now());
        job = jobRepository.save(job);

        final Job finalJob = job;
        registerAfterCommit(() -> schedulerService.triggerNow(finalJob));

        log.info("Manually triggered job id={} by user={}", jobId, username);
        return toResponse(job);
    }

    // ------------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------------

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private void registerAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { action.run(); }
            });
        } else {
            action.run();
        }
    }

    public JobDto.JobResponse toResponse(Job job) {
        return JobDto.JobResponse.builder()
                .id(job.getId())
                .name(job.getName())
                .description(job.getDescription())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .cronExpression(job.getCronExpression())
                .timezone(job.getTimezone())
                .scheduledAt(job.getScheduledAt())
                .payload(job.getPayload())
                .retryCount(job.getRetryCount())
                .maxRetries(job.getMaxRetries())
                .lastExecutedAt(job.getLastExecutedAt())
                .nextFireAt(job.getNextFireAt())
                .completedAt(job.getCompletedAt())
                .lastTriggeredAt(job.getLastTriggeredAt())
                .lastErrorMessage(job.getLastErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .ownerUsername(job.getUser().getUsername())
                .build();
    }

    private JobDto.LogResponse toLogResponse(JobExecutionLog l) {
        return JobDto.LogResponse.builder()
                .id(l.getId())
                .jobId(l.getJob().getId())
                .jobName(l.getJob().getName())
                .jobType(l.getJob().getJobType())
                .status(l.getStatus())
                .executedAt(l.getExecutedAt())
                .completedAt(l.getCompletedAt())
                .errorMessage(l.getErrorMessage())
                .durationMs(l.getDurationMs())
                .attemptNumber(l.getAttemptNumber())
                .build();
    }
}
