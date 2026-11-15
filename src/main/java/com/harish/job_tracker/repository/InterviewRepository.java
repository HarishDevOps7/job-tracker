
package com.harish.job_tracker.repository;

import com.harish.job_tracker.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    // Get all interviews belonging to the logged-in user
    List<Interview> findByJobApplicationUserId(Long userId);

    // Find a specific interview belonging to the logged-in user
    Optional<Interview> findByIdAndJobApplicationUserId(
            Long id,
            Long userId
    );

    // Check whether an interview belongs to a particular application and user
    Optional<Interview> findByIdAndJobApplicationIdAndJobApplicationUserId(
            Long id,
            Long applicationId,
            Long userId
    );
}
