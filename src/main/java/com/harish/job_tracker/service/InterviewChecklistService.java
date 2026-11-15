package com.harish.job_tracker.service;

import com.harish.job_tracker.dto.CreateChecklistItemRequest;
import com.harish.job_tracker.dto.InterviewChecklistItemResponse;
import com.harish.job_tracker.entity.Interview;
import com.harish.job_tracker.entity.InterviewChecklistItem;
import com.harish.job_tracker.repository.InterviewChecklistItemRepository;
import com.harish.job_tracker.repository.InterviewRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class InterviewChecklistService {

    private static final List<String> DEFAULT_TASKS = List.of(
            "Research the company",
            "Review the job description",
            "Prepare technical questions",
            "Prepare HR questions",
            "Review your resume",
            "Test microphone and camera",
            "Keep resume and documents ready"
    );

    private final InterviewRepository interviewRepository;
    private final InterviewChecklistItemRepository checklistRepository;
    private final CurrentUserService currentUserService;

    public InterviewChecklistService(
            InterviewRepository interviewRepository,
            InterviewChecklistItemRepository checklistRepository,
            CurrentUserService currentUserService
    ) {
        this.interviewRepository = interviewRepository;
        this.checklistRepository = checklistRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public List<InterviewChecklistItemResponse> getChecklist(Long interviewId) {
        Interview interview = findOwnedInterview(interviewId);

        List<InterviewChecklistItem> items =
                checklistRepository.findByInterviewIdOrderByIdAsc(interviewId);

        if (items.isEmpty()) {
            for (String title : DEFAULT_TASKS) {
                InterviewChecklistItem item = new InterviewChecklistItem();
                item.setInterview(interview);
                item.setTitle(title);
                checklistRepository.save(item);
            }

            items = checklistRepository.findByInterviewIdOrderByIdAsc(interviewId);
        }

        return items.stream().map(this::toResponse).toList();
    }

    @Transactional
    public InterviewChecklistItemResponse addTask(
            Long interviewId,
            CreateChecklistItemRequest request
    ) {
        Interview interview = findOwnedInterview(interviewId);

        InterviewChecklistItem item = new InterviewChecklistItem();
        item.setInterview(interview);
        item.setTitle(request.title().trim());

        return toResponse(checklistRepository.save(item));
    }

    @Transactional
    public InterviewChecklistItemResponse updateTask(
            Long interviewId,
            Long itemId,
            boolean completed
    ) {
        findOwnedInterview(interviewId);

        InterviewChecklistItem item = checklistRepository
                .findByIdAndInterviewId(itemId, interviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Checklist item not found"
                ));

        item.setCompleted(completed);

        return toResponse(checklistRepository.save(item));
    }

    @Transactional
    public void deleteTask(Long interviewId, Long itemId) {
        findOwnedInterview(interviewId);

        InterviewChecklistItem item = checklistRepository
                .findByIdAndInterviewId(itemId, interviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Checklist item not found"
                ));

        checklistRepository.delete(item);
    }

    @Transactional
    public List<InterviewChecklistItemResponse> resetChecklist(Long interviewId) {
        Interview interview = findOwnedInterview(interviewId);

        checklistRepository.deleteByInterviewId(interviewId);

        for (String title : DEFAULT_TASKS) {
            InterviewChecklistItem item = new InterviewChecklistItem();
            item.setInterview(interview);
            item.setTitle(title);
            checklistRepository.save(item);
        }

        return checklistRepository
                .findByInterviewIdOrderByIdAsc(interviewId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Interview findOwnedInterview(Long interviewId) {
        Long userId = currentUserService.getCurrentUser().getId();

        return interviewRepository
                .findByIdAndJobApplicationUserId(interviewId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Interview not found"
                ));
    }

    private InterviewChecklistItemResponse toResponse(
            InterviewChecklistItem item
    ) {
        return new InterviewChecklistItemResponse(
                item.getId(),
                item.getTitle(),
                item.isCompleted(),
                item.getCompletedAt(),
                item.getCreatedAt()
        );
    }
}