package com.harish.job_tracker.dto;

public record DashboardStatsResponse(
        long totalApplications,
        long applied,
        long assessment,
        long interview,
        long offer,
        long rejected,
        long withdrawn,
        long noResponse
) {}