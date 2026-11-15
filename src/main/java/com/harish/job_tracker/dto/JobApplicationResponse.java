package com.harish.job_tracker.dto;

import com.harish.job_tracker.entity.ApplicationStatus;
import java.time.LocalDate;

public record JobApplicationResponse(
        Long id,
        String companyName,
        String jobTitle,
        String source,
        String jobUrl,
        LocalDate appliedDate,
        ApplicationStatus status
) {}