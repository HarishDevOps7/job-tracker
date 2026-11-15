
package com.harish.job_tracker.dto;

import com.harish.job_tracker.entity.InterviewMode;
import com.harish.job_tracker.entity.InterviewStatus;

import java.time.LocalDateTime;

public record InterviewResponse(

        Long id,

        Long jobApplicationId,

        String companyName,

        String jobTitle,

        String roundName,

        LocalDateTime interviewDateTime,

        InterviewMode mode,

        String interviewerName,

        String meetingLink,

        String notes,

        InterviewStatus status

) {}