
package com.harish.job_tracker.service;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private User user;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    // Test 1: Successfully load user
    @Test
    void loadUserByUsernameSuccessfully() {

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(user.getEmail()).thenReturn("test@gmail.com");
        when(user.getPasswordHash()).thenReturn("encodedPassword");
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.isActive()).thenReturn(true);

        UserDetails result =
                customUserDetailsService.loadUserByUsername("test@gmail.com");

        assertEquals("test@gmail.com", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());

        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        verify(userRepository).findByEmail("test@gmail.com");
    }

    // Test 2: Normalize email to lowercase and trim spaces
    @Test
    void loadUserByUsernameNormalizesEmail() {

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(user.getEmail()).thenReturn("test@gmail.com");
        when(user.getPasswordHash()).thenReturn("encodedPassword");
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.isActive()).thenReturn(true);

        UserDetails result =
                customUserDetailsService
                        .loadUserByUsername(" TEST@GMAIL.COM ");

        assertEquals("test@gmail.com", result.getUsername());

        verify(userRepository).findByEmail("test@gmail.com");
    }

    // Test 3: Throw exception when user does not exist
    @Test
    void loadUserByUsernameThrowsExceptionWhenUserNotFound() {

        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService
                        .loadUserByUsername("unknown@gmail.com")
        );

        verify(userRepository).findByEmail("unknown@gmail.com");
    }

    // Test 4: Return disabled account when user is inactive
    @Test
    void loadUserByUsernameDisabledWhenUserInactive() {

        when(userRepository.findByEmail("inactive@gmail.com"))
                .thenReturn(Optional.of(user));

        when(user.getEmail()).thenReturn("inactive@gmail.com");
        when(user.getPasswordHash()).thenReturn("encodedPassword");
        when(user.getRole()).thenReturn(UserRole.USER);
        when(user.isActive()).thenReturn(false);

        UserDetails result =
                customUserDetailsService
                        .loadUserByUsername("inactive@gmail.com");

        assertFalse(result.isEnabled());
    }
}
