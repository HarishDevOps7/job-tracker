package com.harish.job_tracker.repository;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Existing authentication and registration methods
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    // Admin user management methods
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName, String email);

    List<User> findByRole(UserRole role);

    List<User> findByActive(boolean active);

    long countByActive(boolean active);

    long countByRole(UserRole role);
}