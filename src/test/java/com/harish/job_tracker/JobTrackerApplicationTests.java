package com.harish.job_tracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.harish.job_tracker.config.TestMailConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMailConfiguration.class)
class JobTrackerApplicationTests {

    @Test
    void contextLoads() {
    }
}