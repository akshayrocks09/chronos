package com.chronos.scheduler;

import com.chronos.exception.RetryableJobException;
import com.chronos.service.WorkerService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * The Quartz job bean that gets triggered by the scheduler.
 * Delegates execution to WorkerService, then schedules retries
 * AFTER all transactions have committed to avoid race conditions.
 */
@Component
public class ChronosQuartzJob extends QuartzJobBean {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChronosQuartzJob.class);

    @Autowired
    private WorkerService workerService;

    @Autowired
    private JobSchedulerService schedulerService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long jobId = context.getMergedJobDataMap().getLong("jobId");

        try {
            // executeJob runs in REQUIRES_NEW — by the time it returns (or throws),
            // its transaction is fully committed or rolled back.
            workerService.executeJob(jobId);

        } catch (RetryableJobException ex) {
            // The failure state (RETRYING / FAILED) is already committed to the DB.
            // Now we can safely schedule the next retry trigger without any race condition.
            OffsetDateTime retryAt = ex.getNextRetryAt();
            if (retryAt != null) {
                try {
                    // Load the job fresh to pass to scheduleRetry
                    schedulerService.scheduleRetryById(jobId, retryAt);
                    log.info("[RETRY_SCHEDULED] Job {} retry scheduled at {}", jobId, retryAt);
                } catch (Exception schedEx) {
                    log.error("[RETRY_SCHEDULE_FAILED] Could not schedule retry for job {}: {}", jobId, schedEx.getMessage());
                }
            }
            // else: permanently failed — no retry needed
            log.error("Error in Quartz wrapper for job {}: {}", jobId, ex.getMessage());

        } catch (Exception ex) {
            log.error("Unexpected error in Quartz wrapper for job {}: {}", jobId, ex.getMessage());
        }
    }
}
