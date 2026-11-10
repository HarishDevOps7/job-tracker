
package com.harish.job_tracker.service;

import com.harish.job_tracker.dto.RegisterRequest;
import com.harish.job_tracker.dto.MobileRegisterRequest;
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

    // EMAIL REGISTRATION TESTS

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
        assertFalse(result.isActive());
        assertFalse(result.isEmailVerified());

        verify(userRepository).save(any(User.class));
        verify(otpService).sendEmailOtp(result);
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

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(otpService, never()).sendEmailOtp(any(User.class));
    }

    @Test
    void registerShouldNormalizeEmailAndName() {

        when(userRepository.existsByEmail("harish@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(request);

        assertEquals("Harish Naidu", result.getFullName());
        assertEquals("harish@gmail.com", result.getEmail());
    }

    @Test
    void registerShouldStoreEncodedPassword() {

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

    // MOBILE REGISTRATION TESTS

    @Test
    void registerMobileSuccessfully() {

        MobileRegisterRequest mobileRequest = new MobileRegisterRequest();
        mobileRequest.setFullName(" Harish Naidu ");
        mobileRequest.setMobileNumber("9876543210");

        when(userRepository.existsByMobileNumber("9876543210"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerMobile(mobileRequest);

        assertNotNull(result);
        assertEquals("Harish Naidu", result.getFullName());
        assertEquals("9876543210", result.getMobileNumber());
        assertNull(result.getEmail());
        assertNull(result.getPasswordHash());
        assertEquals(UserRole.USER, result.getRole());
        assertFalse(result.isActive());
        assertFalse(result.isEmailVerified());
        assertFalse(result.isMobileVerified());

        verify(otpService).sendMobileOtp(result);
    }

    @Test
    void registerMobileShouldRemoveCountryCode91() {

        MobileRegisterRequest mobileRequest = new MobileRegisterRequest();
        mobileRequest.setFullName("Harish");
        mobileRequest.setMobileNumber("+91 9876543210");

        when(userRepository.existsByMobileNumber("9876543210"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerMobile(mobileRequest);

        assertEquals("9876543210", result.getMobileNumber());
    }

    @Test
    void registerMobileShouldRejectDuplicateNumber() {

        MobileRegisterRequest mobileRequest = new MobileRegisterRequest();
        mobileRequest.setFullName("Harish");
        mobileRequest.setMobileNumber("9876543210");

        when(userRepository.existsByMobileNumber("9876543210"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.registerMobile(mobileRequest)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(userRepository, never()).save(any(User.class));
        verify(otpService, never()).sendMobileOtp(any(User.class));
    }

    @Test
    void registerMobileShouldRemoveNonDigitCharacters() {

        MobileRegisterRequest mobileRequest = new MobileRegisterRequest();
        mobileRequest.setFullName("Harish");
        mobileRequest.setMobileNumber("987-654-3210");

        when(userRepository.existsByMobileNumber("9876543210"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registerMobile(mobileRequest);

        assertEquals("9876543210", result.getMobileNumber());
    }
}
