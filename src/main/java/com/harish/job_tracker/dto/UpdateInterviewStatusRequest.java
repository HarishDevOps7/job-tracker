
package com.harish.job_tracker.dto;

import com.harish.job_tracker.entity.InterviewStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateInterviewStatusRequest(

        @NotNull(message = "Interview status is required")
        InterviewStatus status

) {}
