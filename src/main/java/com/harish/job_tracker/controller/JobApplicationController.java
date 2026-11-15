
package com.harish.job_tracker.controller;
import com.harish.job_tracker.dto.DashboardStatsResponse;
import com.harish.job_tracker.dto.CreateJobRequest;
import com.harish.job_tracker.dto.DashboardStatsResponse;
import com.harish.job_tracker.dto.JobApplicationResponse;
import com.harish.job_tracker.service.JobApplicationService;
import com.harish.job_tracker.dto.UpdateApplicationStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobApplicationResponse create(
            @Valid @RequestBody CreateJobRequest request) {
        return service.create(request);
    }

    // GET ALL
    @GetMapping
    public List<JobApplicationResponse> findAll() {
        return service.findAll();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public JobApplicationResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public JobApplicationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CreateJobRequest request) {
        return service.update(id, request);
    }

    // DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // UPDATE APPLICATION STATUS
@PatchMapping("/{id}/status")
public JobApplicationResponse updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateApplicationStatusRequest request) {

    return service.updateStatus(id, request);
}

@GetMapping("/stats")
public DashboardStatsResponse getDashboardStats() {
    return service.getDashboardStats();
}

}
