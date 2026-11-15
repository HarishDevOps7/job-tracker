
package com.harish.job_tracker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link interview to an existing job application
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_application_id", nullable = false)
    private JobApplication jobApplication;

    @Column(nullable = false)
    private String roundName;

    @Column(nullable = false)
    private LocalDateTime interviewDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewMode mode;

    private String interviewerName;

    private String meetingLink;

    @Column(length = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @OneToMany(
    mappedBy = "interview",
    cascade = CascadeType.ALL,
    orphanRemoval = true
)
private List<InterviewChecklistItem> checklistItems = new ArrayList<>();

    public Interview() {
    }

    public Long getId() {
        return id;
    }

    public JobApplication getJobApplication() {
        return jobApplication;
    }

    public String getRoundName() {
        return roundName;
    }

    public LocalDateTime getInterviewDateTime() {
        return interviewDateTime;
    }

    public InterviewMode getMode() {
        return mode;
    }

    public String getInterviewerName() {
        return interviewerName;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public String getNotes() {
        return notes;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public void setJobApplication(JobApplication jobApplication) {
        this.jobApplication = jobApplication;
    }

    public void setRoundName(String roundName) {
        this.roundName = roundName;
    }

    public void setInterviewDateTime(LocalDateTime interviewDateTime) {
        this.interviewDateTime = interviewDateTime;
    }

    public void setMode(InterviewMode mode) {
        this.mode = mode;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = interviewerName;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }
}