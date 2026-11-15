
package com.harish.job_tracker.controller;

import com.harish.job_tracker.dto.CreateInterviewRequest;
import com.harish.job_tracker.dto.InterviewResponse;
import com.harish.job_tracker.entity.InterviewStatus;
import com.harish.job_tracker.service.InterviewService;
import com.harish.job_tracker.dto.UpdateInterviewStatusRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    // Schedule a new interview
    @PostMapping
    public ResponseEntity<InterviewResponse> create(
            @Valid @RequestBody CreateInterviewRequest request) {

        InterviewResponse response = interviewService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get all interviews
    @GetMapping
    public ResponseEntity<List<InterviewResponse>> findAll() {

        return ResponseEntity.ok(interviewService.findAll());
    }

    // Get interview by ID
    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> findById(
            @PathVariable Long id) {

        return ResponseEntity.ok(interviewService.findById(id));
    }

    // Update interview details
    @PutMapping("/{id}")
    public ResponseEntity<InterviewResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateInterviewRequest request) {

        return ResponseEntity.ok(
                interviewService.update(id, request)
        );
    }

    // Update interview status
    @PatchMapping("/{id}/status")
    public ResponseEntity<InterviewResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInterviewStatusRequest request) {

        return ResponseEntity.ok(
                interviewService.updateStatus(id, request.status())
        );
    }

    // Delete interview
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        interviewService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
