package com.chronos.exception;

import java.time.OffsetDateTime;

/**
 * Thrown by WorkerService.executeJob() when a job fails and needs a retry.
 * Carries the next retry time so ChronosQuartzJob can schedule it
 * AFTER all transactions have committed.
 */
public class RetryableJobException extends RuntimeException {

    private final OffsetDateTime nextRetryAt;

    public RetryableJobException(OffsetDateTime nextRetryAt, Throwable cause) {
        super(cause.getMessage(), cause);
        this.nextRetryAt = nextRetryAt;
    }

    /** Null means permanently failed (no more retries). */
    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }
}
