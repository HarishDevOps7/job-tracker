
package com.harish.job_tracker.service;

import com.harish.job_tracker.dto.RegisterRequest;
import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {

        // Normalize email before database operations
        String normalizedEmail = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Check duplicate email
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        // Create new user
        User user = new User();

        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);

        // Encrypt password
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        // Default role
        user.setRole(UserRole.USER);

        // New account active by default
        user.setActive(true);

        return userRepository.save(user);
    }
}
