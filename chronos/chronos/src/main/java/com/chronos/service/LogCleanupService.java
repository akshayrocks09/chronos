package com.chronos.service;

import com.chronos.repository.JobExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogCleanupService {

    private final JobExecutionLogRepository logRepository;

    @Value("${chronos.logs.retention-days:30}")
    private int retentionDays;

    /**
     * Run every day at 1 AM to prune old logs.
     * Cron format: "0 0 1 * * ?"
     */
    @Scheduled(cron = "${chronos.logs.cleanup-cron:0 0 1 * * ?}")
    @Transactional
    public void cleanupOldLogs() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
        log.info("Starting automated log cleanup. Removing logs older than {} days (before {})", retentionDays, cutoff);
        
        try {
            logRepository.deleteByExecutedAtBefore(cutoff);
            log.info("Automated log cleanup completed successfully.");
        } catch (Exception e) {
            log.error("Failed to prune old job execution logs: {}", e.getMessage(), e);
        }
    }
}
