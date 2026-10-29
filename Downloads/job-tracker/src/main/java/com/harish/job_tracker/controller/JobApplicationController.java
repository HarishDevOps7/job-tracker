package com.harish.job_tracker.controller;

import com.harish.job_tracker.dto.CreateJobRequest;
import com.harish.job_tracker.entity.JobApplication;
import com.harish.job_tracker.service.JobApplicationService;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobApplication create(
            @Valid @RequestBody CreateJobRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<JobApplication> findAll() {
        return service.findAll();
    }
}