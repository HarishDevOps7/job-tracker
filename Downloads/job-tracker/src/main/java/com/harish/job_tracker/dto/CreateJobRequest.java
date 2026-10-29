package com.harish.job_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreateJobRequest(
        @NotBlank String companyName,
        @NotBlank String jobTitle,
        String source,
        String jobUrl,
        LocalDate appliedDate
) {
}