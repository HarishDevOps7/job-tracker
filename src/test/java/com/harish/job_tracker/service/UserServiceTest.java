
package com.harish.job_tracker.service;
import com.harish.job_tracker.service.OtpService;
import com.harish.job_tracker.dto.RegisterRequest;
import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OtpService otpService;
    @InjectMocks
    private UserService userService;


    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest();
        request.setFullName(" Harish Naidu ");
        request.setEmail(" HARISH@GMAIL.COM ");
        request.setPassword("Password@123");
    }

    @Test
    void registerSuccessfully() {

        when(userRepository.existsByEmail("harish@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password@123"))
                .thenReturn("bcrypt-hash");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(request);

        assertNotNull(result);
        assertEquals("Harish Naidu", result.getFullName());
        assertEquals("harish@gmail.com", result.getEmail());
        assertEquals("bcrypt-hash", result.getPasswordHash());
        assertEquals(UserRole.USER, result.getRole());

        verify(userRepository).existsByEmail("harish@gmail.com");
        verify(passwordEncoder).encode("Password@123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerShouldRejectDuplicateEmail() {

        when(userRepository.existsByEmail("harish@gmail.com"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.register(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(userRepository).existsByEmail("harish@gmail.com");
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerShouldAlwaysAssignUserRole() {

        when(userRepository.existsByEmail("harish@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        userService.register(request);

        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();

        assertEquals(UserRole.USER, savedUser.getRole());
    }

    @Test
    void registerShouldStoreEncodedPasswordNotPlainText() {

        when(userRepository.existsByEmail("harish@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password@123"))
                .thenReturn("bcrypt-hash");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(request);

        assertNotEquals("Password@123", result.getPasswordHash());
        assertEquals("bcrypt-hash", result.getPasswordHash());
    }
}
