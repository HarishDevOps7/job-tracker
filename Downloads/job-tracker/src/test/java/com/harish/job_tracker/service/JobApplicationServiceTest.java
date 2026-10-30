
package com.harish.job_tracker.service;

import com.harish.job_tracker.dto.CreateJobRequest;
import com.harish.job_tracker.entity.ApplicationStatus;
import com.harish.job_tracker.entity.JobApplication;
import com.harish.job_tracker.repository.JobApplicationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository repository;

    @InjectMocks
    private JobApplicationService service;

    // TEST 1: CREATE APPLICATION
    @Test
    void shouldCreateJobApplication() {

        CreateJobRequest request = new CreateJobRequest(
                "TCS",
                "DevOps Engineer",
                "LinkedIn",
                "https://www.tcs.com/careers",
                LocalDate.of(2026, 10, 30)
        );

        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.create(request);

        assertNotNull(result);
        assertEquals("TCS", result.getCompanyName());
        assertEquals("DevOps Engineer", result.getJobTitle());
        assertEquals("LinkedIn", result.getSource());
        assertEquals(ApplicationStatus.APPLIED, result.getStatus());

        verify(repository, times(1)).save(any(JobApplication.class));
    }

    // TEST 2: GET ALL APPLICATIONS
    @Test
    void shouldReturnAllApplications() {

        JobApplication job1 = new JobApplication();
        job1.setCompanyName("TCS");
        job1.setJobTitle("DevOps Engineer");

        JobApplication job2 = new JobApplication();
        job2.setCompanyName("Infosys");
        job2.setJobTitle("Java Developer");

        when(repository.findAll())
                .thenReturn(List.of(job1, job2));

        List<JobApplication> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals("TCS", result.get(0).getCompanyName());
        assertEquals("Infosys", result.get(1).getCompanyName());

        verify(repository, times(1)).findAll();
    }

    // TEST 3: GET APPLICATION BY ID
    @Test
    void shouldReturnApplicationById() {

        JobApplication job = new JobApplication();
        job.setCompanyName("TCS");
        job.setJobTitle("DevOps Engineer");

        when(repository.findById(1L))
                .thenReturn(Optional.of(job));

        JobApplication result = service.findById(1L);

        assertNotNull(result);
        assertEquals("TCS", result.getCompanyName());

        verify(repository).findById(1L);
    }

    // TEST 4: UPDATE APPLICATION
    @Test
    void shouldUpdateApplication() {

        JobApplication existing = new JobApplication();

        existing.setCompanyName("TCS");
        existing.setJobTitle("Java Developer");
        existing.setStatus(ApplicationStatus.APPLIED);

        CreateJobRequest request = new CreateJobRequest(
                "Infosys",
                "DevOps Engineer",
                "LinkedIn",
                "https://www.infosys.com/careers",
                LocalDate.of(2026, 10, 30)
        );

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.update(1L, request);

        assertEquals("Infosys", result.getCompanyName());
        assertEquals("DevOps Engineer", result.getJobTitle());
        assertEquals("LinkedIn", result.getSource());
        assertEquals(ApplicationStatus.APPLIED, result.getStatus());

        verify(repository).save(existing);
    }

    // TEST 5: DELETE APPLICATION
    @Test
    void shouldDeleteApplication() {

        JobApplication existing = new JobApplication();
        existing.setCompanyName("TCS");
        existing.setJobTitle("DevOps Engineer");

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(repository).delete(existing);
    }

    // TEST 6: APPLICATION NOT FOUND
    @Test
    void shouldThrowExceptionWhenApplicationNotFound() {

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> service.findById(999L)
        );

        verify(repository).findById(999L);
    }
}
