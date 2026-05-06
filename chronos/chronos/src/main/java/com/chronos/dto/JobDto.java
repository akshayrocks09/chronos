package com.chronos.dto;

import com.chronos.enums.JobStatus;
import com.chronos.enums.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class JobDto {

    public static class CreateJobRequest {
        @NotBlank(message = "Job name is required")
        private String name;
        private String description;
        @NotNull(message = "Job type is required")
        private JobType jobType;
        private String cronExpression;
        private String timezone = "UTC";
        private OffsetDateTime scheduledAt;
        private String payload;
        private int maxRetries = 3;

        public CreateJobRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public JobType getJobType() { return jobType; }
        public void setJobType(JobType jobType) { this.jobType = jobType; }
        public String getCronExpression() { return cronExpression; }
        public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        public OffsetDateTime getScheduledAt() { return scheduledAt; }
        public void setScheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    }

    public static class UpdateJobRequest {
        private String name;
        private String description;
        private String cronExpression;
        private String timezone;
        private OffsetDateTime scheduledAt;
        private String payload;
        private Integer maxRetries;

        public UpdateJobRequest() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCronExpression() { return cronExpression; }
        public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        public OffsetDateTime getScheduledAt() { return scheduledAt; }
        public void setScheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        public Integer getMaxRetries() { return maxRetries; }
        public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    }

    public static class JobResponse {
        private Long id;
        private String name;
        private String description;
        private JobType jobType;
        private JobStatus status;
        private String cronExpression;
        private String timezone;
        private OffsetDateTime scheduledAt;
        private String payload;
        private int retryCount;
        private int maxRetries;
        private OffsetDateTime lastExecutedAt;
        private OffsetDateTime nextFireAt;
        private OffsetDateTime completedAt;
        private OffsetDateTime lastTriggeredAt;
        private String lastErrorMessage;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private String ownerUsername;

        public JobResponse() {}

        public JobResponse(Long id, String name, String description, JobType jobType, JobStatus status, String cronExpression, String timezone, OffsetDateTime scheduledAt, String payload, int retryCount, int maxRetries, OffsetDateTime lastExecutedAt, OffsetDateTime nextFireAt, OffsetDateTime completedAt, OffsetDateTime lastTriggeredAt, String lastErrorMessage, OffsetDateTime createdAt, OffsetDateTime updatedAt, String ownerUsername) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.jobType = jobType;
            this.status = status;
            this.cronExpression = cronExpression;
            this.timezone = timezone;
            this.scheduledAt = scheduledAt;
            this.payload = payload;
            this.retryCount = retryCount;
            this.maxRetries = maxRetries;
            this.lastExecutedAt = lastExecutedAt;
            this.nextFireAt = nextFireAt;
            this.completedAt = completedAt;
            this.lastTriggeredAt = lastTriggeredAt;
            this.lastErrorMessage = lastErrorMessage;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.ownerUsername = ownerUsername;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public JobType getJobType() { return jobType; }
        public void setJobType(JobType jobType) { this.jobType = jobType; }
        public JobStatus getStatus() { return status; }
        public void setStatus(JobStatus status) { this.status = status; }
        public String getCronExpression() { return cronExpression; }
        public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        public OffsetDateTime getScheduledAt() { return scheduledAt; }
        public void setScheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public OffsetDateTime getLastExecutedAt() { return lastExecutedAt; }
        public void setLastExecutedAt(OffsetDateTime lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; }
        public OffsetDateTime getNextFireAt() { return nextFireAt; }
        public void setNextFireAt(OffsetDateTime nextFireAt) { this.nextFireAt = nextFireAt; }
        public OffsetDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
        public OffsetDateTime getLastTriggeredAt() { return lastTriggeredAt; }
        public void setLastTriggeredAt(OffsetDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }
        public String getLastErrorMessage() { return lastErrorMessage; }
        public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
        public String getOwnerUsername() { return ownerUsername; }
        public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

        public static JobResponseBuilder builder() { return new JobResponseBuilder(); }

        public static class JobResponseBuilder {
            private Long id;
            private String name;
            private String description;
            private JobType jobType;
            private JobStatus status;
            private String cronExpression;
            private String timezone;
            private OffsetDateTime scheduledAt;
            private String payload;
            private int retryCount;
            private int maxRetries;
            private OffsetDateTime lastExecutedAt;
            private OffsetDateTime nextFireAt;
            private OffsetDateTime completedAt;
            private OffsetDateTime lastTriggeredAt;
            private String lastErrorMessage;
            private OffsetDateTime createdAt;
            private OffsetDateTime updatedAt;
            private String ownerUsername;

            public JobResponseBuilder id(Long id) { this.id = id; return this; }
            public JobResponseBuilder name(String name) { this.name = name; return this; }
            public JobResponseBuilder description(String description) { this.description = description; return this; }
            public JobResponseBuilder jobType(JobType jobType) { this.jobType = jobType; return this; }
            public JobResponseBuilder status(JobStatus status) { this.status = status; return this; }
            public JobResponseBuilder cronExpression(String cronExpression) { this.cronExpression = cronExpression; return this; }
            public JobResponseBuilder timezone(String timezone) { this.timezone = timezone; return this; }
            public JobResponseBuilder scheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; return this; }
            public JobResponseBuilder payload(String payload) { this.payload = payload; return this; }
            public JobResponseBuilder retryCount(int retryCount) { this.retryCount = retryCount; return this; }
            public JobResponseBuilder maxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }
            public JobResponseBuilder lastExecutedAt(OffsetDateTime lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; return this; }
            public JobResponseBuilder nextFireAt(OffsetDateTime nextFireAt) { this.nextFireAt = nextFireAt; return this; }
            public JobResponseBuilder completedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; return this; }
            public JobResponseBuilder lastTriggeredAt(OffsetDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; return this; }
            public JobResponseBuilder lastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; return this; }
            public JobResponseBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
            public JobResponseBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
            public JobResponseBuilder ownerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; return this; }

            public JobResponse build() {
                return new JobResponse(id, name, description, jobType, status, cronExpression, timezone, scheduledAt, payload, retryCount, maxRetries, lastExecutedAt, nextFireAt, completedAt, lastTriggeredAt, lastErrorMessage, createdAt, updatedAt, ownerUsername);
            }
        }
    }

    public static class JobStatsResponse {
        private long totalJobs;
        private long pendingJobs;
        private long runningJobs;
        private long completedJobs;
        private long failedJobs;
        private long cancelledJobs;
        private long scheduledJobs;

        public JobStatsResponse() {}

        public JobStatsResponse(long totalJobs, long pendingJobs, long runningJobs, long completedJobs, long failedJobs, long cancelledJobs, long scheduledJobs) {
            this.totalJobs = totalJobs;
            this.pendingJobs = pendingJobs;
            this.runningJobs = runningJobs;
            this.completedJobs = completedJobs;
            this.failedJobs = failedJobs;
            this.cancelledJobs = cancelledJobs;
            this.scheduledJobs = scheduledJobs;
        }

        public long getTotalJobs() { return totalJobs; }
        public void setTotalJobs(long totalJobs) { this.totalJobs = totalJobs; }
        public long getPendingJobs() { return pendingJobs; }
        public void setPendingJobs(long pendingJobs) { this.pendingJobs = pendingJobs; }
        public long getRunningJobs() { return runningJobs; }
        public void setRunningJobs(long runningJobs) { this.runningJobs = runningJobs; }
        public long getCompletedJobs() { return completedJobs; }
        public void setCompletedJobs(long completedJobs) { this.completedJobs = completedJobs; }
        public long getFailedJobs() { return failedJobs; }
        public void setFailedJobs(long failedJobs) { this.failedJobs = failedJobs; }
        public long getCancelledJobs() { return cancelledJobs; }
        public void setCancelledJobs(long cancelledJobs) { this.cancelledJobs = cancelledJobs; }
        public long getScheduledJobs() { return scheduledJobs; }
        public void setScheduledJobs(long scheduledJobs) { this.scheduledJobs = scheduledJobs; }

        public static JobStatsResponseBuilder builder() { return new JobStatsResponseBuilder(); }

        public static class JobStatsResponseBuilder {
            private long totalJobs;
            private long pendingJobs;
            private long runningJobs;
            private long completedJobs;
            private long failedJobs;
            private long cancelledJobs;
            private long scheduledJobs;

            public JobStatsResponseBuilder totalJobs(long totalJobs) { this.totalJobs = totalJobs; return this; }
            public JobStatsResponseBuilder pendingJobs(long pendingJobs) { this.pendingJobs = pendingJobs; return this; }
            public JobStatsResponseBuilder runningJobs(long runningJobs) { this.runningJobs = runningJobs; return this; }
            public JobStatsResponseBuilder completedJobs(long completedJobs) { this.completedJobs = completedJobs; return this; }
            public JobStatsResponseBuilder failedJobs(long failedJobs) { this.failedJobs = failedJobs; return this; }
            public JobStatsResponseBuilder cancelledJobs(long cancelledJobs) { this.cancelledJobs = cancelledJobs; return this; }
            public JobStatsResponseBuilder scheduledJobs(long scheduledJobs) { this.scheduledJobs = scheduledJobs; return this; }

            public JobStatsResponse build() {
                return new JobStatsResponse(totalJobs, pendingJobs, runningJobs, completedJobs, failedJobs, cancelledJobs, scheduledJobs);
            }
        }
    }

    public static class LogResponse {
        private Long id;
        private Long jobId;
        private String jobName;
        private JobType jobType;
        private JobStatus status;
        private OffsetDateTime executedAt;
        private OffsetDateTime completedAt;
        private String errorMessage;
        private Long durationMs;
        private int attemptNumber;

        public LogResponse() {}

        public LogResponse(Long id, Long jobId, String jobName, JobType jobType, JobStatus status, OffsetDateTime executedAt, OffsetDateTime completedAt, String errorMessage, Long durationMs, int attemptNumber) {
            this.id = id;
            this.jobId = jobId;
            this.jobName = jobName;
            this.jobType = jobType;
            this.status = status;
            this.executedAt = executedAt;
            this.completedAt = completedAt;
            this.errorMessage = errorMessage;
            this.durationMs = durationMs;
            this.attemptNumber = attemptNumber;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getJobId() { return jobId; }
        public void setJobId(Long jobId) { this.jobId = jobId; }
        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }
        public JobType getJobType() { return jobType; }
        public void setJobType(JobType jobType) { this.jobType = jobType; }
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

        public static LogResponseBuilder builder() { return new LogResponseBuilder(); }

        public static class LogResponseBuilder {
            private Long id;
            private Long jobId;
            private String jobName;
            private JobType jobType;
            private JobStatus status;
            private OffsetDateTime executedAt;
            private OffsetDateTime completedAt;
            private String errorMessage;
            private Long durationMs;
            private int attemptNumber;

            public LogResponseBuilder id(Long id) { this.id = id; return this; }
            public LogResponseBuilder jobId(Long jobId) { this.jobId = jobId; return this; }
            public LogResponseBuilder jobName(String jobName) { this.jobName = jobName; return this; }
            public LogResponseBuilder jobType(JobType jobType) { this.jobType = jobType; return this; }
            public LogResponseBuilder status(JobStatus status) { this.status = status; return this; }
            public LogResponseBuilder executedAt(OffsetDateTime executedAt) { this.executedAt = executedAt; return this; }
            public LogResponseBuilder completedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; return this; }
            public LogResponseBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
            public LogResponseBuilder durationMs(Long durationMs) { this.durationMs = durationMs; return this; }
            public LogResponseBuilder attemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; return this; }

            public LogResponse build() {
                return new LogResponse(id, jobId, jobName, jobType, status, executedAt, completedAt, errorMessage, durationMs, attemptNumber);
            }
        }
    }
}
