
package com.harish.job_tracker.service;

import com.harish.job_tracker.dto.CreateJobRequest;
import com.harish.job_tracker.entity.ApplicationStatus;
import com.harish.job_tracker.entity.JobApplication;
import com.harish.job_tracker.repository.JobApplicationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository repository;

    @InjectMocks
    private JobApplicationService service;

    private CreateJobRequest request;

    private JobApplication job;

    @BeforeEach
    void setUp() {

        request = new CreateJobRequest(
                "TCS",
                "Java Developer",
                "LinkedIn",
                "https://example.com/job",
                LocalDate.of(2026, 11, 2)
        );

        job = new JobApplication();
        job.setCompanyName("TCS");
        job.setJobTitle("Java Developer");
    }

    // Test 1: Create application successfully
    @Test
    void shouldCreateJobApplication() {

        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.create(request);

        assertNotNull(result);
        assertEquals("TCS", result.getCompanyName());
        assertEquals("Java Developer", result.getJobTitle());
        assertEquals("LinkedIn", result.getSource());
        assertEquals("https://example.com/job", result.getJobUrl());
        assertEquals(LocalDate.of(2026, 11, 2), result.getAppliedDate());
        assertEquals(ApplicationStatus.APPLIED, result.getStatus());

        verify(repository, times(1)).save(any(JobApplication.class));
    }

    // Test 2: Trim company name and job title
    @Test
    void shouldTrimCompanyNameAndJobTitle() {

        CreateJobRequest trimRequest = new CreateJobRequest(
                "  TCS  ",
                "  Java Developer  ",
                "LinkedIn",
                "https://example.com/job",
                LocalDate.now()
        );

        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.create(trimRequest);

        assertEquals("TCS", result.getCompanyName());
        assertEquals("Java Developer", result.getJobTitle());
    }

    // Test 3: Use today's date when applied date is null
    @Test
    void shouldUseCurrentDateWhenAppliedDateIsNull() {

        CreateJobRequest nullDateRequest = new CreateJobRequest(
                "TCS",
                "Java Developer",
                "LinkedIn",
                "https://example.com/job",
                null
        );

        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.create(nullDateRequest);

        assertEquals(LocalDate.now(), result.getAppliedDate());
    }

    // Test 4: Get all applications
    @Test
    void shouldReturnAllApplications() {

        when(repository.findAll())
                .thenReturn(List.of(job));

        List<JobApplication> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals("TCS", result.get(0).getCompanyName());

        verify(repository, times(1)).findAll();
    }

    // Test 5: Find application by ID
    @Test
    void shouldFindApplicationById() {

        when(repository.findById(1L))
                .thenReturn(Optional.of(job));

        JobApplication result = service.findById(1L);

        assertNotNull(result);
        assertEquals("TCS", result.getCompanyName());

        verify(repository).findById(1L);
    }

    // Test 6: Throw exception when application is not found
    @Test
    void shouldThrowExceptionWhenApplicationNotFound() {

        when(repository.findById(100L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findById(100L)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(repository).findById(100L);
    }

    // Test 7: Update application successfully
    @Test
    void shouldUpdateJobApplication() {

        when(repository.findById(1L))
                .thenReturn(Optional.of(job));

        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.update(1L, request);

        assertEquals("TCS", result.getCompanyName());
        assertEquals("Java Developer", result.getJobTitle());
        assertEquals("LinkedIn", result.getSource());
        assertEquals("https://example.com/job", result.getJobUrl());
        assertEquals(LocalDate.of(2026, 11, 2), result.getAppliedDate());

        verify(repository).findById(1L);
        verify(repository).save(job);
    }

    // Test 8: Preserve existing date when update date is null
    @Test
    void shouldPreserveExistingDateWhenUpdateDateIsNull() {

        LocalDate existingDate = LocalDate.of(2026, 10, 20);
        job.setAppliedDate(existingDate);

        CreateJobRequest updateRequest = new CreateJobRequest(
                "Infosys",
                "DevOps Engineer",
                "Naukri",
                "https://example.com/updated-job",
                null
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(job));

        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.update(1L, updateRequest);

        assertEquals("Infosys", result.getCompanyName());
        assertEquals("DevOps Engineer", result.getJobTitle());
        assertEquals(existingDate, result.getAppliedDate());
    }

    // Test 9: Delete application successfully
    @Test
    void shouldDeleteJobApplication() {

        when(repository.findById(1L))
                .thenReturn(Optional.of(job));

        service.delete(1L);

        verify(repository).findById(1L);
        verify(repository).delete(job);
    }

    // Test 10: Do not delete when application is not found
    @Test
    void shouldThrowExceptionWhenDeletingNonExistingApplication() {

        when(repository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> service.delete(100L)
        );

        verify(repository).findById(100L);
        verify(repository, never()).delete(any(JobApplication.class));
    }
}
