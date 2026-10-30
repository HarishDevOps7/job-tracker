
package com.harish.job_tracker.service;

import com.harish.job_tracker.dto.CreateJobRequest;
import com.harish.job_tracker.entity.ApplicationStatus;
import com.harish.job_tracker.entity.JobApplication;
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

    public JobApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    // CREATE
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

    // GET ALL
    @Transactional(readOnly = true)
    public List<JobApplication> findAll() {
        return repository.findAll();
    }

    // GET BY ID
    @Transactional(readOnly = true)
    public JobApplication findById(Long id) {

        return repository.findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Application not found: " + id
                )
            );
    }

    // UPDATE
    @Transactional
    public JobApplication update(Long id, CreateJobRequest request) {

        JobApplication existing = findById(id);

        existing.setCompanyName(request.companyName().trim());
        existing.setJobTitle(request.jobTitle().trim());
        existing.setSource(request.source());
        existing.setJobUrl(request.jobUrl());

        if (request.appliedDate() != null) {
            existing.setAppliedDate(request.appliedDate());
        }

        return repository.save(existing);
    }

    // DELETE
    @Transactional
    public void delete(Long id) {

        JobApplication existing = findById(id);

        repository.delete(existing);
    }
}
