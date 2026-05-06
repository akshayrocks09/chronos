package com.chronos.repository;

import com.chronos.entity.JobExecutionLog;
import com.chronos.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLog, Long> {
    Page<JobExecutionLog> findByJobId(Long jobId, Pageable pageable);
    
    // Global logs for a user
    Page<JobExecutionLog> findByJobUserIdOrderByExecutedAtDesc(Long userId, Pageable pageable);
    Page<JobExecutionLog> findByJobUserIdAndStatusOrderByExecutedAtDesc(Long userId, JobStatus status, Pageable pageable);
    
    // Global Admin Queries
    Page<JobExecutionLog> findAllByOrderByExecutedAtDesc(Pageable pageable);
    
    Page<JobExecutionLog> findByStatusOrderByExecutedAtDesc(JobStatus status, Pageable pageable);

    void deleteByExecutedAtBefore(java.time.OffsetDateTime dateTime);
}
