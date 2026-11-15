package com.harish.job_tracker.controller;

import com.harish.job_tracker.dto.CreateChecklistItemRequest;
import com.harish.job_tracker.dto.InterviewChecklistItemResponse;
import com.harish.job_tracker.dto.UpdateChecklistItemRequest;
import com.harish.job_tracker.service.InterviewChecklistService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviews/{interviewId}/checklist")
public class InterviewChecklistController {

    private final InterviewChecklistService checklistService;

    public InterviewChecklistController(
            InterviewChecklistService checklistService
    ) {
        this.checklistService = checklistService;
    }

    @GetMapping
    public ResponseEntity<List<InterviewChecklistItemResponse>> getChecklist(
            @PathVariable Long interviewId
    ) {
        return ResponseEntity.ok(
                checklistService.getChecklist(interviewId)
        );
    }

    @PostMapping
    public ResponseEntity<InterviewChecklistItemResponse> addTask(
            @PathVariable Long interviewId,
            @Valid @RequestBody CreateChecklistItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                checklistService.addTask(interviewId, request)
        );
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<InterviewChecklistItemResponse> updateTask(
            @PathVariable Long interviewId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateChecklistItemRequest request
    ) {
        return ResponseEntity.ok(
                checklistService.updateTask(
                        interviewId,
                        itemId,
                        request.completed()
                )
        );
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long interviewId,
            @PathVariable Long itemId
    ) {
        checklistService.deleteTask(interviewId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<List<InterviewChecklistItemResponse>> resetChecklist(
            @PathVariable Long interviewId
    ) {
        return ResponseEntity.ok(
                checklistService.resetChecklist(interviewId)
        );
    }
}