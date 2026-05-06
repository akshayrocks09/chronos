package com.chronos.entity;

import com.chronos.enums.JobStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "job_execution_logs", indexes = {
    @Index(name = "idx_logs_job_status", columnList = "job_id, status")
})
public class JobExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime executedAt;

    private OffsetDateTime completedAt;

    @Column(length = 5000)
    private String errorMessage;

    /** Duration of execution in milliseconds */
    private Long durationMs;

    private int attemptNumber;

    public JobExecutionLog() {}

    public JobExecutionLog(Long id, Job job, JobStatus status, OffsetDateTime executedAt, OffsetDateTime completedAt, String errorMessage, Long durationMs, int attemptNumber) {
        this.id = id;
        this.job = job;
        this.status = status;
        this.executedAt = executedAt;
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.attemptNumber = attemptNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public OffsetDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(OffsetDateTime executedAt) { this.executedAt = executedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }

    @PrePersist
    protected void onCreate() {
        executedAt = OffsetDateTime.now();
    }

    public static JobExecutionLogBuilder builder() {
        return new JobExecutionLogBuilder();
    }

    public static class JobExecutionLogBuilder {
        private Long id;
        private Job job;
        private JobStatus status;
        private OffsetDateTime executedAt;
        private OffsetDateTime completedAt;
        private String errorMessage;
        private Long durationMs;
        private int attemptNumber;

        public JobExecutionLogBuilder id(Long id) { this.id = id; return this; }
        public JobExecutionLogBuilder job(Job job) { this.job = job; return this; }
        public JobExecutionLogBuilder status(JobStatus status) { this.status = status; return this; }
        public JobExecutionLogBuilder executedAt(OffsetDateTime executedAt) { this.executedAt = executedAt; return this; }
        public JobExecutionLogBuilder completedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; return this; }
        public JobExecutionLogBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public JobExecutionLogBuilder durationMs(Long durationMs) { this.durationMs = durationMs; return this; }
        public JobExecutionLogBuilder attemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; return this; }

        public JobExecutionLog build() {
            return new JobExecutionLog(id, job, status, executedAt, completedAt, errorMessage, durationMs, attemptNumber);
        }
    }
}
