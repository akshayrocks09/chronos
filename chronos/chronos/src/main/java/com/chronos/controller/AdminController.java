package com.chronos.controller;

import com.chronos.dto.ApiResponse;
import com.chronos.dto.JobDto;
import com.chronos.enums.JobStatus;
import com.chronos.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin-only endpoints for system-wide monitoring and management")
public class AdminController {

    private final JobService jobService;

    /**
     * List all jobs across all users with filtering and pagination.
     */
    @GetMapping("/jobs")
    @Operation(summary = "List all jobs across all users with optional filter (admin only)")
    public ResponseEntity<ApiResponse<Page<JobDto.JobResponse>>> getAllJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Page<JobDto.JobResponse> jobs = jobService.getAllSystemJobs(
                status, search, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/logs")
    @Operation(summary = "Get all execution logs across the entire system (Admin only)")
    public ResponseEntity<ApiResponse<Page<JobDto.LogResponse>>> getAllLogs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<JobDto.LogResponse> logs = jobService.getAllSystemLogs(status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get system-wide job statistics (Admin only)")
    public ResponseEntity<ApiResponse<JobDto.JobStatsResponse>> getSystemStats() {
        JobDto.JobStatsResponse stats = jobService.getSystemStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
