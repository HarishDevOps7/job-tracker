
package com.harish.job_tracker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_challenges")
public class VerificationChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false, length = 10)
    private String channel;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "reset_token_hash")
private String resetTokenHash;

@Column(name = "reset_token_expires_at")
private LocalDateTime resetTokenExpiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public VerificationChallenge() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getDestination() {
        return destination;
    }

    public String getChannel() {
        return channel;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getResetTokenHash() {
    return resetTokenHash;
}

public LocalDateTime getResetTokenExpiresAt() {
    return resetTokenExpiresAt;
}

public void setResetTokenHash(String resetTokenHash) {
    this.resetTokenHash = resetTokenHash;
}

public void setResetTokenExpiresAt(LocalDateTime resetTokenExpiresAt) {
    this.resetTokenExpiresAt = resetTokenExpiresAt;
}
}
