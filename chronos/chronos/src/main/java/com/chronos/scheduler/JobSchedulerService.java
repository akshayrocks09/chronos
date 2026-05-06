package com.chronos.scheduler;

import com.chronos.entity.Job;
import com.chronos.enums.JobType;
import com.chronos.exception.JobSchedulingException;
import com.chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobSchedulerService {

    private final Scheduler scheduler;
    private final JobRepository jobRepository;


    /**
     * Schedule a newly created job in Quartz.
     */
    public String scheduleJob(Job job) {
        try {
            String jobKey = "job-" + job.getId();
            String triggerKey = "trigger-" + job.getId();

            JobDetail jobDetail = buildJobDetail(job, jobKey);
            Trigger trigger = buildTrigger(job, jobKey, triggerKey);

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled job id={} key={} timezone={}", job.getId(), jobKey, job.getTimezone());
            return jobKey;

        } catch (SchedulerException e) {
            throw new JobSchedulingException("Failed to schedule job: " + e.getMessage(), e);
        }
    }

    /**
     * Reschedule an existing job with updated parameters.
     */
    public void rescheduleJob(Job job) {
        try {
            String jobKey = job.getQuartzJobKey();
            String triggerKey = "trigger-" + job.getId();

            JobDetail jobDetail = buildJobDetail(job, jobKey);
            TriggerKey tk = TriggerKey.triggerKey(triggerKey, "chronos");

            // Always update the job definition first (replace existing)
            scheduler.addJob(jobDetail, true);

            if (scheduler.checkExists(tk)) {
                Trigger newTrigger = buildTrigger(job, jobKey, triggerKey);
                scheduler.rescheduleJob(tk, newTrigger);
                log.info("Rescheduled job id={}", job.getId());
            } else {
                // Trigger doesn't exist yet — schedule fresh trigger for existing job
                Trigger trigger = buildTrigger(job, jobKey, triggerKey);
                scheduler.scheduleJob(trigger);
                log.info("Scheduled new trigger for updated job id={}", job.getId());
            }
        } catch (SchedulerException e) {
            throw new JobSchedulingException("Failed to reschedule job: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel/delete a job from Quartz.
     */
    public void cancelJob(Job job) {
        try {
            String jobKey = job.getQuartzJobKey();
            if (jobKey != null) {
                JobKey jk = JobKey.jobKey(jobKey, "chronos");
                if (scheduler.checkExists(jk)) {
                    scheduler.deleteJob(jk);
                    log.info("Cancelled Quartz job id={}", job.getId());
                }
            }
        } catch (SchedulerException e) {
            throw new JobSchedulingException("Failed to cancel job: " + e.getMessage(), e);
        }
    }

    /**
     * Trigger a job to run immediately (for manual fire).
     */
    public void triggerNow(Job job) {
        try {
            String jobKey = job.getQuartzJobKey();
            if (jobKey != null) {
                scheduler.triggerJob(JobKey.jobKey(jobKey, "chronos"));
                log.info("Manually triggered job id={}", job.getId());
            }
        } catch (SchedulerException e) {
            throw new JobSchedulingException("Failed to trigger job: " + e.getMessage(), e);
        }
    }

    /**
     * Get the next fire time for a job from Quartz.
     */
    public OffsetDateTime getNextFireTime(Job job) {
        try {
            String triggerKey = "trigger-" + job.getId();
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(triggerKey, "chronos"));
            if (trigger != null && trigger.getNextFireTime() != null) {
                return trigger.getNextFireTime().toInstant().atOffset(ZoneOffset.UTC);
            }
        } catch (SchedulerException e) {
            log.warn("Failed to get next fire time for job {}: {}", job.getId(), e.getMessage());
        }
        return calculateNextFireTime(job);
    }

    /**
     * Calculate what the next fire time SHOULD be based on job configuration.
     * Useful during job creation before it's actually in Quartz.
     */
    public OffsetDateTime calculateNextFireTime(Job job) {
        if (job.getJobType() == JobType.ONE_TIME) {
            return job.getScheduledAt();
        }
        
        if (job.getJobType() == JobType.RECURRING && job.getCronExpression() != null) {
            try {
                CronExpression cron = new CronExpression(job.getCronExpression());
                // Set timezone for cron calculation
                cron.setTimeZone(TimeZone.getTimeZone(job.getTimezone() != null ? job.getTimezone() : "UTC"));
                
                Date next = cron.getNextValidTimeAfter(new Date());
                if (next != null) {
                    return next.toInstant().atOffset(ZoneOffset.UTC);
                }
            } catch (Exception e) {
                log.warn("Failed to parse cron for next fire time: {}", job.getCronExpression());
            }
        }
        return null;
    }

    /**
     * Schedule a one-time retry attempt for a failed job.
     */
    public void scheduleRetry(Job job, OffsetDateTime nextFireAt) {
        try {
            String jobKey = job.getQuartzJobKey();
            if (jobKey == null) {
                jobKey = "job-" + job.getId();
            }
            String triggerKey = "retry-" + job.getId() + "-" + System.currentTimeMillis();

            Date startDate = Date.from(nextFireAt.toInstant());

            Trigger retryTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey, "chronos")
                    .forJob(jobKey, "chronos")
                    .startAt(startDate)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow())
                    .build();

            scheduler.scheduleJob(retryTrigger);
            log.info("Scheduled retry for job id={} at {}", job.getId(), nextFireAt);

        } catch (SchedulerException e) {
            throw new JobSchedulingException("Failed to schedule retry: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Convenience method for ChronosQuartzJob: load the job by ID and schedule a retry.
     * Called OUTSIDE any transaction so the DB state is fully committed before Quartz fires.
     */
    public void scheduleRetryById(Long jobId, OffsetDateTime nextFireAt) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobSchedulingException("Job not found for retry scheduling: " + jobId));
        scheduleRetry(job, nextFireAt);
    }


    private JobDetail buildJobDetail(Job job, String jobKey) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("jobId", job.getId());

        return JobBuilder.newJob(ChronosQuartzJob.class)
                .withIdentity(jobKey, "chronos")
                .withDescription(job.getName())
                .usingJobData(dataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildTrigger(Job job, String jobKey, String triggerKey) {
        if (job.getJobType() == JobType.RECURRING) {
            return buildCronTrigger(job, jobKey, triggerKey);
        } else {
            return buildSimpleTrigger(job, jobKey, triggerKey);
        }
    }

    private Trigger buildCronTrigger(Job job, String jobKey, String triggerKey) {
        if (job.getCronExpression() == null || job.getCronExpression().isBlank()) {
            throw new JobSchedulingException("Cron expression is required for RECURRING jobs");
        }
        try {
            TimeZone tz = TimeZone.getTimeZone(job.getTimezone() != null ? job.getTimezone() : "UTC");
            return TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey, "chronos")
                    .forJob(jobKey, "chronos")
                    .withSchedule(CronScheduleBuilder
                            .cronSchedule(job.getCronExpression())
                            .inTimeZone(tz)
                            .withMisfireHandlingInstructionFireAndProceed())
                    .build();
        } catch (Exception e) {
            throw new JobSchedulingException("Invalid cron expression: " + job.getCronExpression(), e);
        }
    }

    private Trigger buildSimpleTrigger(Job job, String jobKey, String triggerKey) {
        OffsetDateTime fireAt = job.getScheduledAt();

        // If no schedule or schedule is in the past, fire immediately
        if (fireAt == null || fireAt.isBefore(OffsetDateTime.now())) {
            return TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey, "chronos")
                    .forJob(jobKey, "chronos")
                    .startNow()
                    .build();
        }

        Date startDate = Date.from(fireAt.toInstant());
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerKey, "chronos")
                .forJob(jobKey, "chronos")
                .startAt(startDate)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow())
                .build();
    }
}
