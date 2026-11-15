package com.harish.job_tracker.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateChecklistItemRequest(
        @NotNull(message = "Completion status is required")
        Boolean completed
) {}