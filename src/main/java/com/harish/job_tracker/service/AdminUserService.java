
package com.harish.job_tracker.service;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Search users by name or email
    public List<User> searchUsers(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return getAllUsers();
        }

        String search = keyword.trim();

        return userRepository
                .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        search, search);
    }

    // Filter users by role
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    // Filter users by active status
    public List<User> getUsersByStatus(boolean active) {
        return userRepository.findByActive(active);
    }

    // Activate or deactivate a user
    @Transactional
    public User updateUserStatus(Long userId, boolean active) {

        User user = findUser(userId);

        // Prevent admin from deactivating their own account
        if (!active && isCurrentUser(user)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You cannot deactivate your own admin account");
        }

        // Prevent deactivation of the last active administrator
        if (!active
                && user.isActive()
                && user.getRole() == UserRole.ADMIN) {

            validateAnotherActiveAdminExists(user);
        }

        user.setActive(active);

        return userRepository.save(user);
    }

    // Change user role
    @Transactional
    public User updateUserRole(Long userId, UserRole role) {

        User user = findUser(userId);

        if (role == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role cannot be empty");
        }

        // Prevent admin from demoting themselves
        if (isCurrentUser(user) && role != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You cannot remove your own admin role");
        }

        // Prevent removal of the last active administrator
        if (user.isActive()
                && user.getRole() == UserRole.ADMIN
                && role != UserRole.ADMIN) {

            validateAnotherActiveAdminExists(user);
        }

        user.setRole(role);

        return userRepository.save(user);
    }

    // Check whether another active administrator exists
    private void validateAnotherActiveAdminExists(User targetUser) {

        long activeAdminCount = userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == UserRole.ADMIN)
                .filter(User::isActive)
                .filter(user -> !user.getId().equals(targetUser.getId()))
                .count();

        if (activeAdminCount == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot remove the last active administrator");
        }
    }

    // Check whether target user is the currently authenticated user
    private boolean isCurrentUser(User user) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return false;
        }

        return authentication.getName()
                .equalsIgnoreCase(user.getEmail());
    }

    // Dashboard statistics
    public long getTotalUsers() {
        return userRepository.count();
    }

    public long getActiveUsers() {
        return userRepository.countByActive(true);
    }

    public long getInactiveUsers() {
        return userRepository.countByActive(false);
    }

    public long getAdminCount() {
        return userRepository.countByRole(UserRole.ADMIN);
    }

    // Find user by ID
    private User findUser(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));
    }
}
