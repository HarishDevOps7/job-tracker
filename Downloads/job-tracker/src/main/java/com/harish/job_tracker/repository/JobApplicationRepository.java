package com.harish.job_tracker.repository;

import com.harish.job_tracker.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long> {
}