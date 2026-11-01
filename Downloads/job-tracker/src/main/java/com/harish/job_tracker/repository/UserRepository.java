package com.harish.job_tracker.repository;

import com.harish.job_tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(
            com.harish.job_tracker.entity.UserRole role
    );
}