
package com.harish.job_tracker.controller;

import com.harish.job_tracker.dto.AdminUserResponse;
import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.service.AdminUserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminUserService adminUserService;

    public AdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    // Convert User entity into safe response DTO
    private AdminUserResponse toResponse(User user) {

        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    // 1. Get all users
    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {

        List<AdminUserResponse> users =
                adminUserService.getAllUsers()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(users);
    }

    // 2. Search users by name or email
    @GetMapping("/search")
    public ResponseEntity<List<AdminUserResponse>> searchUsers(
            @RequestParam String keyword) {

        List<AdminUserResponse> users =
                adminUserService.searchUsers(keyword)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(users);
    }

    // 3. Get users by role
    @GetMapping("/role/{role}")
    public ResponseEntity<List<AdminUserResponse>> getUsersByRole(
            @PathVariable UserRole role) {

        List<AdminUserResponse> users =
                adminUserService.getUsersByRole(role)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(users);
    }

    // 4. Get users by active/inactive status
    @GetMapping("/status/{active}")
    public ResponseEntity<List<AdminUserResponse>> getUsersByStatus(
            @PathVariable boolean active) {

        List<AdminUserResponse> users =
                adminUserService.getUsersByStatus(active)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(users);
    }

    // 5. Activate or deactivate a user
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        User updatedUser =
                adminUserService.updateUserStatus(id, active);

        return ResponseEntity.ok(toResponse(updatedUser));
    }

    // 6. Change user role
    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserResponse> updateUserRole(
            @PathVariable Long id,
            @RequestParam UserRole role) {

        User updatedUser =
                adminUserService.updateUserRole(id, role);

        return ResponseEntity.ok(toResponse(updatedUser));
    }

    // 7. Get user statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getUserStats() {

        List<User> users = adminUserService.getAllUsers();

        long totalUsers = users.size();

        long activeUsers = users.stream()
                .filter(User::isActive)
                .count();

        long inactiveUsers = totalUsers - activeUsers;

        long adminCount = users.stream()
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .count();

        Map<String, Long> stats = Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "inactiveUsers", inactiveUsers,
                "adminCount", adminCount
        );

        return ResponseEntity.ok(stats);
    }
}
