
package com.harish.job_tracker.service;

import com.harish.job_tracker.dto.CreateJobRequest;
import com.harish.job_tracker.dto.JobApplicationResponse;
import com.harish.job_tracker.entity.ApplicationStatus;
import com.harish.job_tracker.entity.JobApplication;
import com.harish.job_tracker.entity.User;
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

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private JobApplicationService service;

    private CreateJobRequest request;
    private JobApplication job;
    private User user;

    @BeforeEach
    void setUp() {

        request = new CreateJobRequest(
                "TCS",
                "Java Developer",
                "LinkedIn",
                "https://example.com/job",
                LocalDate.of(2026, 11, 2)
        );

        user = mock(User.class);

        job = new JobApplication();
        job.setCompanyName("TCS");
        job.setJobTitle("Java Developer");
        job.setSource("LinkedIn");
        job.setJobUrl("https://example.com/job");
        job.setAppliedDate(LocalDate.of(2026, 11, 2));
        job.setStatus(ApplicationStatus.APPLIED);
    }

    // Common authenticated user setup
    private void mockCurrentUser() {

        when(currentUserService.getCurrentUser())
                .thenReturn(user);

        // ID is needed for ownership-based repository queries.
        // Lenient avoids unnecessary-stubbing errors in CREATE tests.
        lenient().when(user.getId()).thenReturn(10L);
    }

    // Test 1: Create application successfully
    @Test
    void shouldCreateJobApplication() {

        mockCurrentUser();

        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplicationResponse result = service.create(request);

        assertNotNull(result);
        assertEquals("TCS", result.companyName());
        assertEquals("Java Developer", result.jobTitle());
        assertEquals("LinkedIn", result.source());
        assertEquals("https://example.com/job", result.jobUrl());
        assertEquals(LocalDate.of(2026, 11, 2), result.appliedDate());
        assertEquals(ApplicationStatus.APPLIED, result.status());

        verify(repository).save(any(JobApplication.class));
    }

    // Test 2: Trim company name and job title
    @Test
    void shouldTrimCompanyNameAndJobTitle() {

        mockCurrentUser();

        CreateJobRequest trimRequest = new CreateJobRequest(
                "  TCS  ",
                "  Java Developer  ",
                "LinkedIn",
                "https://example.com/job",
                LocalDate.now()
        );

        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplicationResponse result = service.create(trimRequest);

        assertEquals("TCS", result.companyName());
        assertEquals("Java Developer", result.jobTitle());
    }

    // Test 3: Use today's date when applied date is null
    @Test
    void shouldUseCurrentDateWhenAppliedDateIsNull() {

        mockCurrentUser();

        CreateJobRequest nullDateRequest = new CreateJobRequest(
                "TCS",
                "Java Developer",
                "LinkedIn",
                "https://example.com/job",
                null
        );

        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplicationResponse result = service.create(nullDateRequest);

        assertEquals(LocalDate.now(), result.appliedDate());
    }

    // Test 4: Get all applications for current user
    @Test
    void shouldReturnAllApplications() {

        mockCurrentUser();

        when(repository.findByUserId(10L))
                .thenReturn(List.of(job));

        List<JobApplicationResponse> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals("TCS", result.get(0).companyName());

        verify(repository).findByUserId(10L);
    }

    // Test 5: Find application by ID
    @Test
    void shouldFindApplicationById() {

        mockCurrentUser();

        when(repository.findByIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(job));

        JobApplicationResponse result = service.findById(1L);

        assertNotNull(result);
        assertEquals("TCS", result.companyName());

        verify(repository).findByIdAndUserId(1L, 10L);
    }

    // Test 6: Application not found
    @Test
    void shouldThrowExceptionWhenApplicationNotFound() {

        mockCurrentUser();

        when(repository.findByIdAndUserId(100L, 10L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findById(100L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(repository).findByIdAndUserId(100L, 10L);
    }

    // Test 7: Update application successfully
    @Test
    void shouldUpdateJobApplication() {

        mockCurrentUser();

        when(repository.findByIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(job));

        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplicationResponse result = service.update(1L, request);

        assertEquals("TCS", result.companyName());
        assertEquals("Java Developer", result.jobTitle());
        assertEquals("LinkedIn", result.source());
        assertEquals("https://example.com/job", result.jobUrl());
        assertEquals(LocalDate.of(2026, 11, 2), result.appliedDate());

        verify(repository).save(job);
    }

    // Test 8: Preserve existing date when update date is null
    @Test
    void shouldPreserveExistingDateWhenUpdateDateIsNull() {

        mockCurrentUser();

        LocalDate existingDate = LocalDate.of(2026, 10, 20);
        job.setAppliedDate(existingDate);

        CreateJobRequest updateRequest = new CreateJobRequest(
                "Infosys",
                "DevOps Engineer",
                "Naukri",
                "https://example.com/updated-job",
                null
        );

        when(repository.findByIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(job));

        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplicationResponse result = service.update(1L, updateRequest);

        assertEquals("Infosys", result.companyName());
        assertEquals("DevOps Engineer", result.jobTitle());
        assertEquals(existingDate, result.appliedDate());
    }

    // Test 9: Delete application successfully
    @Test
    void shouldDeleteJobApplication() {

        mockCurrentUser();

        when(repository.findByIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(job));

        service.delete(1L);

        verify(repository).findByIdAndUserId(1L, 10L);
        verify(repository).delete(job);
    }

    // Test 10: Do not delete when application is not found
    @Test
    void shouldThrowExceptionWhenDeletingNonExistingApplication() {

        mockCurrentUser();

        when(repository.findByIdAndUserId(100L, 10L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> service.delete(100L)
        );

        verify(repository).findByIdAndUserId(100L, 10L);
        verify(repository, never()).delete(any(JobApplication.class));
    }
}
