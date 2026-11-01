package com.harish.job_tracker.config;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    @Value("${app.admin.email:admin@jobtracker.com}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initializeAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (userRepository.existsByRole(UserRole.ADMIN)) {
                System.out.println("Admin account already exists.");
                return;
            }

            if (adminPassword == null || adminPassword.isBlank()) {
                System.out.println(
                    "Admin account not created. ADMIN_PASSWORD is missing."
                );
                return;
            }

            User admin = new User();

            admin.setFullName(adminName);
            admin.setEmail(adminEmail);
            admin.setPasswordHash(
                passwordEncoder.encode(adminPassword)
            );
            admin.setRole(UserRole.ADMIN);

            userRepository.save(admin);

            System.out.println("Admin account created successfully.");
        };
    }
}