
package com.harish.job_tracker.dto;

import com.harish.job_tracker.entity.InterviewMode;
import com.harish.job_tracker.entity.InterviewStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateInterviewRequest(

        @NotNull(message = "Job application ID is required")
        @Positive(message = "Invalid job application ID")
        Long jobApplicationId,

        @NotBlank(message = "Interview round is required")
        String roundName,

        @NotNull(message = "Interview date and time are required")
        LocalDateTime interviewDateTime,

        @NotNull(message = "Interview mode is required")
        InterviewMode mode,

        String interviewerName,

        String meetingLink,

        @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
        String notes,

        InterviewStatus status

) {}
