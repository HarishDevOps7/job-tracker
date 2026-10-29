
package com.harish.job_tracker.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String jobTitle;

    private String source;
    private String jobUrl;
    private LocalDate appliedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    public JobApplication() {
    }

    public Long getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getSource() {
        return source;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setCompanyName(String value) {
        this.companyName = value;
    }

    public void setJobTitle(String value) {
        this.jobTitle = value;
    }

    public void setSource(String value) {
        this.source = value;
    }

    public void setJobUrl(String value) {
        this.jobUrl = value;
    }

    public void setAppliedDate(LocalDate value) {
        this.appliedDate = value;
    }

    public void setStatus(ApplicationStatus value) {
        this.status = value;
    }
}
