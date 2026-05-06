package com.chronos.service;

import com.chronos.dto.JobDto;
import com.chronos.entity.Job;
import com.chronos.entity.User;
import com.chronos.enums.JobStatus;
import com.chronos.enums.JobType;
import com.chronos.enums.UserRole;
import com.chronos.exception.ResourceNotFoundException;
import com.chronos.repository.JobExecutionLogRepository;
import com.chronos.repository.JobRepository;
import com.chronos.repository.UserRepository;
import com.chronos.scheduler.JobSchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock JobRepository jobRepository;
    @Mock JobExecutionLogRepository logRepository;
    @Mock UserRepository userRepository;
    @Mock JobSchedulerService schedulerService;

    @InjectMocks JobService jobService;

    private User user;
    private Job sampleJob;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).username("alice").email("alice@example.com")
                .password("encoded").role(UserRole.ROLE_USER).enabled(true).build();

        sampleJob = Job.builder()
                .id(10L).name("Test Job").jobType(JobType.ONE_TIME)
                .status(JobStatus.PENDING).maxRetries(3).retryCount(0)
                .scheduledAt(OffsetDateTime.now().plusMinutes(5))
                .timezone("UTC")
                .quartzJobKey("job-10")
                .user(user).build();

        ReflectionTestUtils.setField(jobService, "maxJobsPerUser", 50);
        ReflectionTestUtils.setField(jobService, "triggerCooldownSeconds", 30);
    }

    @Test
    void createOneTimeJob_success() {
        JobDto.CreateJobRequest req = new JobDto.CreateJobRequest();
        req.setName("Test Job");
        req.setJobType(JobType.ONE_TIME);
        req.setScheduledAt(OffsetDateTime.now().plusMinutes(10));
        req.setMaxRetries(3);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jobRepository.save(any(Job.class))).thenReturn(sampleJob);
        when(schedulerService.scheduleJob(any(Job.class))).thenReturn("job-10");

        JobDto.JobResponse response = jobService.createJob("alice", req);

        assertThat(response.getName()).isEqualTo("Test Job");
        verify(schedulerService).scheduleJob(any(Job.class));
    }

    @Test
    void createRecurringJob_missingCron_throws() {
        JobDto.CreateJobRequest req = new JobDto.CreateJobRequest();
        req.setName("Recurring Job");
        req.setJobType(JobType.RECURRING);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> jobService.createJob("alice", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cronExpression");
    }

    @Test
    void getJobById_notFound_throws() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jobRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById("alice", 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelJob_success() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jobRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleJob));
        when(jobRepository.save(any(Job.class))).thenReturn(sampleJob);

        jobService.cancelJob("alice", 10L, false);

        verify(schedulerService).cancelJob(sampleJob);
        assertThat(sampleJob.getStatus()).isEqualTo(JobStatus.CANCELLED);
    }

    @Test
    void cancelJob_alreadyCancelled_throws() {
        sampleJob.setStatus(JobStatus.CANCELLED);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jobRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleJob));

        assertThatThrownBy(() -> jobService.cancelJob("alice", 10L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void cancelJob_completed_throws() {
        sampleJob.setStatus(JobStatus.COMPLETED);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jobRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleJob));

        assertThatThrownBy(() -> jobService.cancelJob("alice", 10L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a completed job");
    }

    @Test
    void updateJob_retrying_throws() {
        sampleJob.setStatus(JobStatus.RETRYING);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jobRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleJob));

        assertThatThrownBy(() -> jobService.updateJob("alice", 10L, new JobDto.UpdateJobRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retrying");
    }

    @Test
    void triggerJobNow_completedJob_throws() {
        sampleJob.setStatus(JobStatus.COMPLETED);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jobRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleJob));

        assertThatThrownBy(() -> jobService.triggerJobNow("alice", 10L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void triggerJobNow_cancelledJob_throws() {
        sampleJob.setStatus(JobStatus.CANCELLED);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jobRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sampleJob));

        assertThatThrownBy(() -> jobService.triggerJobNow("alice", 10L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelled");
    }
}
