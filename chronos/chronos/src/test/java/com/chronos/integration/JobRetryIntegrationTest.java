package com.chronos.integration;

import com.chronos.entity.Job;
import com.chronos.entity.User;
import com.chronos.enums.JobStatus;
import com.chronos.enums.JobType;
import com.chronos.enums.UserRole;
import com.chronos.repository.JobRepository;
import com.chronos.repository.UserRepository;
import com.chronos.scheduler.JobSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test: verifies that a job with {"fail":true} payload
 * transitions to RETRYING after the first execution attempt.
 *
 * FIX: previously this test would fail because WorkerService.process() did not
 * handle "fail":true — it fell through to the LOG_MESSAGE branch and succeeded.
 * Now process() checks for fail=true and throws, which triggers the retry path.
 */
@SpringBootTest
@ActiveProfiles("test")
public class JobRetryIntegrationTest {

    @Autowired private JobRepository jobRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JobSchedulerService schedulerService;

    @BeforeEach
    void cleanUp() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void jobWithFailPayload_transitionsToRetrying() {
        // Arrange — user + intentionally-failing job
        User user = userRepository.save(User.builder()
                .username("tester")
                .email("tester@example.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .enabled(true)
                .build());

        Job job = jobRepository.saveAndFlush(Job.builder()
                .name("Failing Integration Test Job")
                .jobType(JobType.ONE_TIME)
                .status(JobStatus.PENDING)
                .payload("{\"fail\":true}")   // triggers simulated failure in WorkerService
                .maxRetries(3)
                .retryCount(0)
                .user(user)
                .build());

        // Set quartzJobKey and register with Quartz
        String quartzKey = schedulerService.scheduleJob(job);
        job.setQuartzJobKey(quartzKey);
        job = jobRepository.saveAndFlush(job);

        // Act — trigger immediately and wait for the job to fail and enter RETRYING
        schedulerService.triggerNow(job);

        final Long jobId = job.getId();
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                Job updated = jobRepository.findById(jobId).orElseThrow();
                assertThat(updated.getStatus()).isEqualTo(JobStatus.RETRYING);
                assertThat(updated.getRetryCount()).isEqualTo(1);
                assertThat(updated.getLastErrorMessage()).contains("Simulated failure");
                assertThat(updated.getNextFireAt()).isNotNull();
            });
    }

    @Test
    public void completedJob_cannotBeRetriggered_viaScheduler() {
        // Arrange
        User user = userRepository.save(User.builder()
                .username("tester2")
                .email("tester2@example.com")
                .password("password")
                .role(UserRole.ROLE_USER)
                .enabled(true)
                .build());

        Job job = jobRepository.saveAndFlush(Job.builder()
                .name("Log Job")
                .jobType(JobType.ONE_TIME)
                .status(JobStatus.PENDING)
                .payload("{\"type\":\"LOG_MESSAGE\",\"message\":\"hello\"}")
                .maxRetries(3)
                .retryCount(0)
                .user(user)
                .build());

        String quartzKey = schedulerService.scheduleJob(job);
        job.setQuartzJobKey(quartzKey);
        job = jobRepository.saveAndFlush(job);
        schedulerService.triggerNow(job);

        final Long jobId = job.getId();

        // Wait for completion
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                Job updated = jobRepository.findById(jobId).orElseThrow();
                assertThat(updated.getStatus()).isEqualTo(JobStatus.COMPLETED);
            });

        // Verify: if Quartz fires again (e.g. duplicate trigger), WorkerService skips it
        schedulerService.triggerNow(jobRepository.findById(jobId).orElseThrow());

        // Status must remain COMPLETED — the duplicate trigger is a no-op
        await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(300, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                Job updated = jobRepository.findById(jobId).orElseThrow();
                assertThat(updated.getStatus()).isEqualTo(JobStatus.COMPLETED);
            });
    }
}
