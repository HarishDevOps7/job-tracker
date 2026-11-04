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

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        User user = new User();

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);

        // Never store plain-text passwords
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        // Public registration always creates a normal user
        user.setRole(UserRole.USER);

        return userRepository.save(user);
    }
}