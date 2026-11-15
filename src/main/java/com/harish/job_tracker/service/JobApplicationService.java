
package com.harish.job_tracker.service;
import com.harish.job_tracker.dto.DashboardStatsResponse;
import com.harish.job_tracker.dto.CreateJobRequest;
import com.harish.job_tracker.dto.JobApplicationResponse;
import com.harish.job_tracker.dto.UpdateApplicationStatusRequest;
import com.harish.job_tracker.entity.ApplicationStatus;
import com.harish.job_tracker.entity.JobApplication;
import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.repository.JobApplicationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

@Service
public class JobApplicationService {

    private final JobApplicationRepository repository;
    private final CurrentUserService currentUserService;

    public JobApplicationService(
            JobApplicationRepository repository,
            CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    // CREATE
    @Transactional
    public JobApplicationResponse create(CreateJobRequest request) {

        User user = currentUserService.getCurrentUser();

        JobApplication job = new JobApplication();

        job.setCompanyName(request.companyName().trim());
        job.setJobTitle(request.jobTitle().trim());
        job.setSource(request.source());
        job.setJobUrl(request.jobUrl());

        job.setAppliedDate(
                request.appliedDate() != null
                        ? request.appliedDate()
                        : LocalDate.now()
        );

        job.setStatus(ApplicationStatus.APPLIED);
        job.setUser(user);

        return toResponse(repository.save(job));
    }

    // GET ALL - CURRENT USER ONLY
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> findAll() {

        User user = currentUserService.getCurrentUser();

        return repository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // GET BY ID - CURRENT USER ONLY
    @Transactional(readOnly = true)
    public JobApplicationResponse findById(Long id) {

        return toResponse(findEntityById(id));
    }

    // UPDATE APPLICATION DETAILS
    @Transactional
    public JobApplicationResponse update(
            Long id,
            CreateJobRequest request) {

        JobApplication existing = findEntityById(id);

        existing.setCompanyName(request.companyName().trim());
        existing.setJobTitle(request.jobTitle().trim());
        existing.setSource(request.source());
        existing.setJobUrl(request.jobUrl());

        if (request.appliedDate() != null) {
            existing.setAppliedDate(request.appliedDate());
        }

        return toResponse(repository.save(existing));
    }

    // UPDATE APPLICATION STATUS
    @Transactional
    public JobApplicationResponse updateStatus(
            Long id,
            UpdateApplicationStatusRequest request) {

        JobApplication existing = findEntityById(id);

        existing.setStatus(request.status());

        return toResponse(repository.save(existing));
    }

    // DELETE
    @Transactional
    public void delete(Long id) {

        JobApplication existing = findEntityById(id);
        repository.delete(existing);
    }

    // FIND ENTITY FOR CURRENT USER
    private JobApplication findEntityById(Long id) {

        User user = currentUserService.getCurrentUser();

        return repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Application not found"
                        )
                );
    }

    // CONVERT ENTITY TO RESPONSE DTO
    private JobApplicationResponse toResponse(JobApplication job) {

        return new JobApplicationResponse(
                job.getId(),
                job.getCompanyName(),
                job.getJobTitle(),
                job.getSource(),
                job.getJobUrl(),
                job.getAppliedDate(),
                job.getStatus()
        );
    }

     
    // DASHBOARD STATISTICS - CURRENT USER ONLY
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {

        User user = currentUserService.getCurrentUser();
        Long userId = user.getId();

        return new DashboardStatsResponse(
                repository.countByUserId(userId),

                repository.countByUserIdAndStatus(
                        userId, ApplicationStatus.APPLIED),

                repository.countByUserIdAndStatus(
                        userId, ApplicationStatus.ASSESSMENT),

                repository.countByUserIdAndStatus(
                        userId, ApplicationStatus.INTERVIEW),

                repository.countByUserIdAndStatus(
                        userId, ApplicationStatus.OFFER),

                repository.countByUserIdAndStatus(
                        userId, ApplicationStatus.REJECTED),

                repository.countByUserIdAndStatus(
                        userId, ApplicationStatus.WITHDRAWN),

                repository.countByUserIdAndStatus(
                        userId, ApplicationStatus.NO_RESPONSE)
        );
    }


}
