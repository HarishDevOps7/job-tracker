
package com.harish.job_tracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Welcome to Job Tracker Application!";
    }

    @GetMapping("/health")
    public String health() {
        return "Job Tracker Application is Running!";
    }
}
