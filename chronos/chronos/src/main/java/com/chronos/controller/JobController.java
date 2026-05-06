package com.chronos.controller;

import com.chronos.dto.ApiResponse;
import com.chronos.dto.JobDto;
import com.chronos.entity.JobExecutionLog;
import com.chronos.enums.JobStatus;
import com.chronos.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Jobs", description = "Create, view, manage and monitor jobs")
public class JobController {

    private final JobService jobService;

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @PostMapping
    @Operation(summary = "Submit a new job (one-time or recurring)")
    public ResponseEntity<ApiResponse<JobDto.JobResponse>> createJob(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody JobDto.CreateJobRequest request) {
        JobDto.JobResponse job = jobService.createJob(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(job, "Job created successfully"));
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    @GetMapping
    @Operation(summary = "List all my jobs")
    public ResponseEntity<ApiResponse<Page<JobDto.JobResponse>>> getMyJobs(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<JobDto.JobResponse> jobs = jobService.getUserJobs(principal.getUsername(), status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/logs")
    @Operation(summary = "Get execution logs for only the authenticated user's jobs")
    public ResponseEntity<ApiResponse<Page<JobDto.LogResponse>>> getMyLogs(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<JobDto.LogResponse> logs = jobService.getUserLogs(principal.getUsername(), status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get my job statistics")
    public ResponseEntity<ApiResponse<JobDto.JobStatsResponse>> getStats(
            @AuthenticationPrincipal UserDetails principal) {
        JobDto.JobStatsResponse stats = jobService.getUserStats(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get a specific job by ID")
    public ResponseEntity<ApiResponse<JobDto.JobResponse>> getJob(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long jobId) {
        JobDto.JobResponse job = jobService.getJobById(principal.getUsername(), jobId);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @GetMapping("/{jobId}/logs")
    @Operation(summary = "Get execution logs for a job (last 10 runs)")
    public ResponseEntity<ApiResponse<List<JobExecutionLog>>> getJobLogs(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long jobId) {
        Page<JobExecutionLog> logs = jobService.getJobLogs(principal.getUsername(), jobId, 10);
        return ResponseEntity.ok(ApiResponse.success(logs.getContent()));
    }

    // ------------------------------------------------------------------
    // Update / Reschedule
    // ------------------------------------------------------------------

    @PutMapping("/{jobId}")
    @Operation(summary = "Update/reschedule an existing job")
    public ResponseEntity<ApiResponse<JobDto.JobResponse>> updateJob(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long jobId,
            @RequestBody JobDto.UpdateJobRequest request) {
        JobDto.JobResponse job = jobService.updateJob(principal.getUsername(), jobId, request);
        return ResponseEntity.ok(ApiResponse.success(job, "Job updated successfully"));
    }

    // ------------------------------------------------------------------
    // Cancel
    // ------------------------------------------------------------------

    @PatchMapping("/{jobId}/cancel")
    @Operation(summary = "Cancel a job while keeping its history")
    public ResponseEntity<ApiResponse<JobDto.JobResponse>> cancelJob(
            @AuthenticationPrincipal UserDetails principal,
            Authentication auth,
            @PathVariable Long jobId) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        JobDto.JobResponse job = jobService.cancelJob(principal.getUsername(), jobId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(job, "Job cancelled successfully"));
    }

    @DeleteMapping("/{jobId}")
    @Operation(summary = "Delete a job permanently")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @AuthenticationPrincipal UserDetails principal,
            Authentication auth,
            @PathVariable Long jobId) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        jobService.deleteJob(principal.getUsername(), jobId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(null, "Job deleted successfully"));
    }

    // ------------------------------------------------------------------
    // Manual trigger
    // ------------------------------------------------------------------

    @PostMapping("/{jobId}/trigger")
    @Operation(summary = "Manually trigger a job to run immediately")
    public ResponseEntity<ApiResponse<JobDto.JobResponse>> triggerNow(
            @AuthenticationPrincipal UserDetails principal,
            Authentication auth,
            @PathVariable Long jobId) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        JobDto.JobResponse job = jobService.triggerJobNow(principal.getUsername(), jobId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(job, "Job triggered successfully"));
    }
}
