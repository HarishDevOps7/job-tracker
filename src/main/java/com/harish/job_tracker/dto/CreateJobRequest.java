
package com.harish.job_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record CreateJobRequest(

        @NotBlank(message = "Company name is required")
        String companyName,

        @NotBlank(message = "Job title is required")
        String jobTitle,

        String source,

        String jobUrl,

        @NotNull(message = "Applied date is required")
        @PastOrPresent(message = "Applied date cannot be in the future")
        LocalDate appliedDate

) {}
