package com.chronos.repository;

import com.chronos.entity.Job;
import com.chronos.enums.JobStatus;
import com.chronos.enums.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // User-scoped queries
    Page<Job> findByUserId(Long userId, Pageable pageable);
    Optional<Job> findByIdAndUserId(Long id, Long userId);
    Page<Job> findByUserIdAndStatus(Long userId, JobStatus status, Pageable pageable);
    Page<Job> findByUserIdAndNameContainingIgnoreCase(Long userId, String search, Pageable pageable);
    Page<Job> findByUserIdAndStatusAndNameContainingIgnoreCase(Long userId, JobStatus status, String search, Pageable pageable);

    // Admin / cross-user queries
    Page<Job> findByStatus(JobStatus status, Pageable pageable);
    Page<Job> findByNameContainingIgnoreCase(String search, Pageable pageable);
    Page<Job> findByStatusAndNameContainingIgnoreCase(JobStatus status, String search, Pageable pageable);

    // Resync: jobs that should be active in Quartz
    List<Job> findByStatusIn(List<JobStatus> statuses);

    // Count helpers
    long countByStatus(JobStatus status);
    long countByUserId(Long userId);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.user.id = :userId AND j.status = :status")
    long countByUserIdAndStatus(Long userId, JobStatus status);

    long countByUserIdAndStatusIn(Long userId, Collection<JobStatus> statuses);

    // Scheduler helpers
    List<Job> findByJobTypeAndStatus(JobType jobType, JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.scheduledAt <= :now AND j.status = 'PENDING' AND j.jobType = 'ONE_TIME'")
    List<Job> findDueOneTimeJobs(OffsetDateTime now);

    @Query("SELECT j FROM Job j WHERE j.status = 'FAILED' AND j.retryCount >= j.maxRetries AND j.user.id = :userId")
    List<Job> findPermanentlyFailedJobsByUser(Long userId);
}
