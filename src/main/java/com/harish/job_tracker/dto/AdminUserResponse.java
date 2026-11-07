
package com.harish.job_tracker.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        String role,
        boolean active,
        LocalDateTime createdAt
) {
}
