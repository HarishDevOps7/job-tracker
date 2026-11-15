
package com.harish.job_tracker.dto;

import com.harish.job_tracker.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(

        @NotNull(message = "Application status is required")
        ApplicationStatus status

) {}
