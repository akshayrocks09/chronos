package com.chronos.scheduler;

import com.chronos.entity.Job;
import com.chronos.enums.JobStatus;
import com.chronos.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * On startup, re-registers any active jobs that lost their Quartz trigger
 * due to a crash or restart (in-memory store doesn't survive restarts).
 *
 * FIX: previously called findAll() which loaded every job in the DB.
 * Now uses findByStatusIn() with only the three active statuses.
 * Also guards against jobs where quartzJobKey was never saved (crash during create).
 */
@Service
public class JobResyncService {

    private static final Logger log = LoggerFactory.getLogger(JobResyncService.class);

    private static final List<JobStatus> ACTIVE_STATUSES = List.of(
            JobStatus.PENDING, JobStatus.SCHEDULED, JobStatus.RETRYING);

    private final JobRepository jobRepository;
    private final JobSchedulerService schedulerService;

    public JobResyncService(JobRepository jobRepository, JobSchedulerService schedulerService) {
        this.jobRepository = jobRepository;
        this.schedulerService = schedulerService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resyncJobs() {
        List<Job> activeJobs = jobRepository.findByStatusIn(ACTIVE_STATUSES);

        if (activeJobs.isEmpty()) {
            log.info("[RESYNC] No active jobs found — nothing to resync.");
            return;
        }

        log.info("[RESYNC] Found {} active jobs to resync with Quartz.", activeJobs.size());
        int success = 0;
        int skipped = 0;

        for (Job job : activeJobs) {
            if (job.getQuartzJobKey() == null || job.getQuartzJobKey().isBlank()) {
                // Job was created but crashed before quartzJobKey was saved.
                // Assign the key now so it can be registered.
                job.setQuartzJobKey("job-" + job.getId());
                jobRepository.save(job);
                log.warn("[RESYNC] Job {} had no quartzJobKey — assigned and saving before resync.", job.getId());
            }
            try {
                schedulerService.rescheduleJob(job);
                success++;
            } catch (Exception e) {
                log.error("[RESYNC] Failed to resync job {}: {}", job.getId(), e.getMessage());
                skipped++;
            }
        }
        log.info("[RESYNC] Complete — {} resynced, {} failed.", success, skipped);
    }
}
