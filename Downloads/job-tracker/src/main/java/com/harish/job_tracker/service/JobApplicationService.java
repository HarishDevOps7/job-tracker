package com.harish.job_tracker.service;

import com.harish.job_tracker.dto.CreateJobRequest;
import com.harish.job_tracker.entity.ApplicationStatus;
import com.harish.job_tracker.entity.JobApplication;
import com.harish.job_tracker.repository.JobApplicationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class JobApplicationService {

    private final JobApplicationRepository repository;

    public JobApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public JobApplication create(CreateJobRequest request) {
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

        return repository.save(job);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findAll() {
        return repository.findAll();
    }
}