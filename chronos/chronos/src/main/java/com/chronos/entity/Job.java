package com.chronos.entity;

import com.chronos.enums.JobStatus;
import com.chronos.enums.JobType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_jobs_status_next_fire", columnList = "status, next_fire_at")
})
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    private String cronExpression;
    
    @Column(nullable = false)
    private String timezone = "UTC";

    private OffsetDateTime scheduledAt;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private String quartzJobKey;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(nullable = false)
    private int maxRetries = 3;

    private OffsetDateTime lastExecutedAt;
    private OffsetDateTime nextFireAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime lastTriggeredAt; // For cooldown tracking

    private String lastErrorMessage;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<JobExecutionLog> executionLogs;

    public Job() {}

    private Job(Long id, String name, String description, JobType jobType, JobStatus status, String cronExpression, String timezone, OffsetDateTime scheduledAt, String payload, String quartzJobKey, int retryCount, int maxRetries, OffsetDateTime lastExecutedAt, OffsetDateTime nextFireAt, OffsetDateTime completedAt, OffsetDateTime lastTriggeredAt, String lastErrorMessage, OffsetDateTime createdAt, OffsetDateTime updatedAt, User user, List<JobExecutionLog> executionLogs) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.jobType = jobType;
        this.status = status;
        this.cronExpression = cronExpression;
        this.timezone = timezone;
        this.scheduledAt = scheduledAt;
        this.payload = payload;
        this.quartzJobKey = quartzJobKey;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.lastExecutedAt = lastExecutedAt;
        this.nextFireAt = nextFireAt;
        this.completedAt = completedAt;
        this.lastTriggeredAt = lastTriggeredAt;
        this.lastErrorMessage = lastErrorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.user = user;
        this.executionLogs = executionLogs;
    }

    // Getters and Setters

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

    public String getQuartzJobKey() { return quartzJobKey; }
    public void setQuartzJobKey(String quartzJobKey) { this.quartzJobKey = quartzJobKey; }

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

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<JobExecutionLog> getExecutionLogs() { return executionLogs; }
    public void setExecutionLogs(List<JobExecutionLog> executionLogs) { this.executionLogs = executionLogs; }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public static JobBuilder builder() {
        return new JobBuilder();
    }

    public static class JobBuilder {
        private Long id;
        private String name;
        private String description;
        private JobType jobType;
        private JobStatus status;
        private String cronExpression;
        private String timezone = "UTC";
        private OffsetDateTime scheduledAt;
        private String payload;
        private String quartzJobKey;
        private int retryCount;
        private int maxRetries;
        private OffsetDateTime lastExecutedAt;
        private OffsetDateTime nextFireAt;
        private OffsetDateTime completedAt;
        private OffsetDateTime lastTriggeredAt;
        private String lastErrorMessage;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private User user;
        private List<JobExecutionLog> executionLogs;

        public JobBuilder id(Long id) { this.id = id; return this; }
        public JobBuilder name(String name) { this.name = name; return this; }
        public JobBuilder description(String description) { this.description = description; return this; }
        public JobBuilder jobType(JobType jobType) { this.jobType = jobType; return this; }
        public JobBuilder status(JobStatus status) { this.status = status; return this; }
        public JobBuilder cronExpression(String cronExpression) { this.cronExpression = cronExpression; return this; }
        public JobBuilder timezone(String timezone) { this.timezone = timezone; return this; }
        public JobBuilder scheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; return this; }
        public JobBuilder payload(String payload) { this.payload = payload; return this; }
        public JobBuilder quartzJobKey(String quartzJobKey) { this.quartzJobKey = quartzJobKey; return this; }
        public JobBuilder retryCount(int retryCount) { this.retryCount = retryCount; return this; }
        public JobBuilder maxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }
        public JobBuilder lastExecutedAt(OffsetDateTime lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; return this; }
        public JobBuilder nextFireAt(OffsetDateTime nextFireAt) { this.nextFireAt = nextFireAt; return this; }
        public JobBuilder completedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; return this; }
        public JobBuilder lastTriggeredAt(OffsetDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; return this; }
        public JobBuilder lastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; return this; }
        public JobBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public JobBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public JobBuilder user(User user) { this.user = user; return this; }
        public JobBuilder executionLogs(List<JobExecutionLog> executionLogs) { this.executionLogs = executionLogs; return this; }

        public Job build() {
            return new Job(id, name, description, jobType, status, cronExpression, timezone, scheduledAt, payload, quartzJobKey, retryCount, maxRetries, lastExecutedAt, nextFireAt, completedAt, lastTriggeredAt, lastErrorMessage, createdAt, updatedAt, user, executionLogs);
        }
    }
}
