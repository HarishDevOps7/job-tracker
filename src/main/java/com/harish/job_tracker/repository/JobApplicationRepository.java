
package com.harish.job_tracker.repository;

import com.harish.job_tracker.entity.ApplicationStatus;
import com.harish.job_tracker.entity.JobApplication;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long> {

    // Fetch applications belonging to a particular user
    List<JobApplication> findByUserId(Long userId);

    // Fetch one application only if it belongs to that user
    Optional<JobApplication> findByIdAndUserId(
            Long id,
            Long userId
    );

    // Count total applications for a user
    long countByUserId(Long userId);

    // Count applications by user and status
    long countByUserIdAndStatus(
            Long userId,
            ApplicationStatus status
    );
}
