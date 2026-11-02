
package com.harish.job_tracker.config;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        adminInitializer = new AdminInitializer();

        ReflectionTestUtils.setField(
            adminInitializer, "adminName", "System Administrator"
        );

        ReflectionTestUtils.setField(
            adminInitializer, "adminEmail", "admin@jobtracker.com"
        );
    }

    
@Test
void shouldNotCreateAdminWhenPasswordIsNull() throws Exception {

    ReflectionTestUtils.setField(
        adminInitializer, "adminPassword", null
    );

    when(userRepository.existsByRole(UserRole.ADMIN))
        .thenReturn(false);

    CommandLineRunner runner =
        adminInitializer.initializeAdmin(userRepository, passwordEncoder);

    runner.run();

    verify(userRepository).existsByRole(UserRole.ADMIN);
    verify(userRepository, never()).save(any(User.class));
    verify(passwordEncoder, never()).encode(anyString());
}


    @Test
    void shouldNotCreateAdminWhenAdminAlreadyExists() throws Exception {

        ReflectionTestUtils.setField(
            adminInitializer, "adminPassword", "Admin@123"
        );

        when(userRepository.existsByRole(UserRole.ADMIN))
            .thenReturn(true);

        CommandLineRunner runner =
            adminInitializer.initializeAdmin(userRepository, passwordEncoder);

        runner.run();

        verify(userRepository).existsByRole(UserRole.ADMIN);
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldNotCreateAdminWhenPasswordIsMissing() throws Exception {

        ReflectionTestUtils.setField(
            adminInitializer, "adminPassword", ""
        );

        when(userRepository.existsByRole(UserRole.ADMIN))
            .thenReturn(false);

        CommandLineRunner runner =
            adminInitializer.initializeAdmin(userRepository, passwordEncoder);

        runner.run();

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldCreateAdminSuccessfully() throws Exception {

        ReflectionTestUtils.setField(
            adminInitializer, "adminPassword", "Admin@123"
        );

        when(userRepository.existsByRole(UserRole.ADMIN))
            .thenReturn(false);

        when(passwordEncoder.encode("Admin@123"))
            .thenReturn("encodedPassword");

        CommandLineRunner runner =
            adminInitializer.initializeAdmin(userRepository, passwordEncoder);

        runner.run();

        verify(userRepository).save(argThat(user ->
            user.getFullName().equals("System Administrator")
            && user.getEmail().equals("admin@jobtracker.com")
            && user.getPasswordHash().equals("encodedPassword")
            && user.getRole() == UserRole.ADMIN
        ));
    }
}
