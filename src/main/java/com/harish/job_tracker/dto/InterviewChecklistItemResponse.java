package com.harish.job_tracker.dto;

import java.time.LocalDateTime;

public record InterviewChecklistItemResponse(
        Long id,
        String title,
        boolean completed,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {}