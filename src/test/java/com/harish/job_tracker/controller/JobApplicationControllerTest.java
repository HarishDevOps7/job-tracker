
package com.harish.job_tracker.controller;

import com.harish.job_tracker.dto.CreateJobRequest;
import com.harish.job_tracker.entity.JobApplication;
import com.harish.job_tracker.service.JobApplicationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import org.springframework.context.annotation.Import;
import com.harish.job_tracker.config.TestMailConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestMailConfiguration.class)

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobApplicationService jobApplicationService;

    // Sample request
    private CreateJobRequest createRequest() {
        return new CreateJobRequest(
                "TCS",
                "Java Developer",
                "LinkedIn",
                "https://example.com/job",
                LocalDate.now()
        );
    }

    // Sample response
    private JobApplication createResponse() {
        JobApplication job = new JobApplication();
        job.setCompanyName("TCS");
        job.setJobTitle("Java Developer");
        return job;
    }

    // Test 1: Create job application
    @Test
    void shouldCreateJobApplication() throws Exception {

        when(jobApplicationService.create(any(CreateJobRequest.class)))
                .thenReturn(createResponse());

        mockMvc.perform(post("/api/applications")
                        .with(user("testuser").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("TCS"))
                .andExpect(jsonPath("$.jobTitle").value("Java Developer"));

        verify(jobApplicationService, times(1))
                .create(any(CreateJobRequest.class));
    }

    // Test 2: Get all job applications
    @Test
    void shouldGetAllApplications() throws Exception {

        when(jobApplicationService.findAll())
                .thenReturn(List.of(createResponse()));

        mockMvc.perform(get("/api/applications")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].companyName").value("TCS"));

        verify(jobApplicationService, times(1)).findAll();
    }

    // Test 3: Get job application by ID
    @Test
    void shouldGetApplicationById() throws Exception {

        when(jobApplicationService.findById(1L))
                .thenReturn(createResponse());

        mockMvc.perform(get("/api/applications/1")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("TCS"))
                .andExpect(jsonPath("$.jobTitle").value("Java Developer"));

        verify(jobApplicationService).findById(1L);
    }

    // Test 4: Update job application
    @Test
    void shouldUpdateApplication() throws Exception {

        when(jobApplicationService.update(
                eq(1L), any(CreateJobRequest.class)))
                .thenReturn(createResponse());

        mockMvc.perform(put("/api/applications/1")
                        .with(user("testuser").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("TCS"))
                .andExpect(jsonPath("$.jobTitle").value("Java Developer"));

        verify(jobApplicationService)
                .update(eq(1L), any(CreateJobRequest.class));
    }

    // Test 5: Delete job application
    @Test
    void shouldDeleteApplication() throws Exception {

        doNothing().when(jobApplicationService).delete(1L);

        mockMvc.perform(delete("/api/applications/1")
                        .with(user("testuser").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(jobApplicationService, times(1)).delete(1L);
    }

    // Test 6: Reject unauthenticated request
    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {

        mockMvc.perform(get("/api/applications")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
