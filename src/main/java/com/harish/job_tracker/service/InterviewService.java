
package com.harish.job_tracker.service;

import com.harish.job_tracker.dto.CreateInterviewRequest;
import com.harish.job_tracker.dto.InterviewResponse;
import com.harish.job_tracker.entity.Interview;
import com.harish.job_tracker.entity.InterviewStatus;
import com.harish.job_tracker.entity.JobApplication;
import com.harish.job_tracker.repository.InterviewRepository;
import com.harish.job_tracker.repository.JobApplicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CurrentUserService currentUserService;

    public InterviewService(
            InterviewRepository interviewRepository,
            JobApplicationRepository jobApplicationRepository,
            CurrentUserService currentUserService) {

        this.interviewRepository = interviewRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.currentUserService = currentUserService;
    }

    // Schedule a new interview
    @Transactional
    public InterviewResponse create(CreateInterviewRequest request) {

        var user = currentUserService.getCurrentUser();

        JobApplication application = jobApplicationRepository
                .findByIdAndUserId(
                        request.jobApplicationId(),
                        user.getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job application not found"
                ));

        Interview interview = new Interview();

        interview.setJobApplication(application);
        interview.setRoundName(request.roundName());
        interview.setInterviewDateTime(request.interviewDateTime());
        interview.setMode(request.mode());
        interview.setInterviewerName(request.interviewerName());
        interview.setMeetingLink(request.meetingLink());
        interview.setNotes(request.notes());

        interview.setStatus(
                request.status() != null
                        ? request.status()
                        : InterviewStatus.SCHEDULED
        );

        Interview saved = interviewRepository.save(interview);

        return toResponse(saved);
    }

    // Get all interviews of logged-in user
    @Transactional(readOnly = true)
    public List<InterviewResponse> findAll() {

        var user = currentUserService.getCurrentUser();

        return interviewRepository
                .findByJobApplicationUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Get one interview
    @Transactional(readOnly = true)
    public InterviewResponse findById(Long id) {

        Interview interview = findEntityById(id);

        return toResponse(interview);
    }

    // Update interview
    @Transactional
    public InterviewResponse update(
            Long id,
            CreateInterviewRequest request) {

        var user = currentUserService.getCurrentUser();

        Interview interview = findEntityById(id);

        JobApplication application = jobApplicationRepository
                .findByIdAndUserId(
                        request.jobApplicationId(),
                        user.getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job application not found"
                ));

        interview.setJobApplication(application);
        interview.setRoundName(request.roundName());
        interview.setInterviewDateTime(request.interviewDateTime());
        interview.setMode(request.mode());
        interview.setInterviewerName(request.interviewerName());
        interview.setMeetingLink(request.meetingLink());
        interview.setNotes(request.notes());

        if (request.status() != null) {
            interview.setStatus(request.status());
        }

        return toResponse(interviewRepository.save(interview));
    }

    // Update interview status
    @Transactional
    public InterviewResponse updateStatus(
            Long id,
            InterviewStatus status) {

        Interview interview = findEntityById(id);

        interview.setStatus(status);

        return toResponse(interviewRepository.save(interview));
    }

    // Delete interview
    @Transactional
    public void delete(Long id) {

        Interview interview = findEntityById(id);

        interviewRepository.delete(interview);
    }

    // Verify interview ownership
    private Interview findEntityById(Long id) {

        var user = currentUserService.getCurrentUser();

        return interviewRepository
                .findByIdAndJobApplicationUserId(
                        id,
                        user.getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Interview not found"
                ));
    }

    // Convert entity to response DTO
    private InterviewResponse toResponse(Interview interview) {

        JobApplication application = interview.getJobApplication();

        return new InterviewResponse(
                interview.getId(),
                application.getId(),
                application.getCompanyName(),
                application.getJobTitle(),
                interview.getRoundName(),
                interview.getInterviewDateTime(),
                interview.getMode(),
                interview.getInterviewerName(),
                interview.getMeetingLink(),
                interview.getNotes(),
                interview.getStatus()
        );
    }
}
