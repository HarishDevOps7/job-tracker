package com.harish.job_tracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.context.annotation.Import;
import com.harish.job_tracker.config.TestMailConfiguration;


@SpringBootTest
@Import(TestMailConfiguration.class)
class JobTrackerApplicationTests { 
	@Test
	void contextLoads() {
	}

}
