package com.harish.job_tracker.repository;

import com.harish.job_tracker.entity.InterviewChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewChecklistItemRepository
        extends JpaRepository<InterviewChecklistItem, Long> {

    List<InterviewChecklistItem> findByInterviewIdOrderByIdAsc(
            Long interviewId
    );

    Optional<InterviewChecklistItem> findByIdAndInterviewId(
            Long id,
            Long interviewId
    );

    long countByInterviewId(Long interviewId);

    void deleteByInterviewId(Long interviewId);
}