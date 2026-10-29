package com.harish.job_tracker.controller;

import com.harish.job_tracker.dto.CreateJobRequest;
import com.harish.job_tracker.entity.JobApplication;
import com.harish.job_tracker.service.JobApplicationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(JobApplicationController.class)
class JobApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobApplicationService jobApplicationService;

    @Test
    void shouldCreateJobApplication() throws Exception {
        CreateJobRequest request = new CreateJobRequest(
                "TCS",
                "Java Developer",
                "LinkedIn",
                "https://example.com/job",
                LocalDate.now()
        );

        JobApplication response = new JobApplication();
        response.setCompanyName("TCS");
        response.setJobTitle("Java Developer");

        when(jobApplicationService.create(any(CreateJobRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("TCS"))
                .andExpect(jsonPath("$.jobTitle").value("Java Developer"));
    }
}