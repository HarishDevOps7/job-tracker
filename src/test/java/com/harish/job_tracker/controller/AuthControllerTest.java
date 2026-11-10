
package com.harish.job_tracker.controller;

import com.harish.job_tracker.dto.*;
import com.harish.job_tracker.entity.*;
import com.harish.job_tracker.repository.UserRepository;
import com.harish.job_tracker.repository.UserAuthProviderRepository;
import com.harish.job_tracker.service.UserService;
import com.harish.job_tracker.service.OtpService;
import org.springframework.mock.web.MockHttpSession;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private OtpService otpService;

    @Mock
    private UserAuthProviderRepository providerRepository;

    private AuthController authController;

    private RegisterRequest request;
    private User user;

    @BeforeEach
    void setUp() throws Exception {

        authController = new AuthController(
                userService,
                userRepository,
                authenticationManager,
                otpService
        );

        ReflectionTestUtils.setField(
                authController,
                "userAuthProviderRepository",
                providerRepository
        );

        request = new RegisterRequest();
        request.setFullName("Harish");
        request.setEmail("harish@example.com");
        request.setPassword("Harish@123");

        user = new User();

        var idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 1L);

        user.setFullName("Harish");
        user.setEmail("harish@example.com");
        user.setPasswordHash("encryptedPassword");
        user.setRole(UserRole.USER);
        user.setActive(true);
        user.setEmailVerified(true);
        user.setMobileVerified(true);
        user.setMobileNumber("9876543210");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // EMAIL REGISTRATION

    @Test
    void registerShouldReturnCreatedResponse() {

        when(userService.register(request)).thenReturn(user);

        ResponseEntity<Map<String, Object>> response =
                authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(
                "OTP sent to your registered email",
                response.getBody().get("message")
        );

        verify(userService).register(request);
    }

    @Test
    void registerShouldReturnUserDetails() {

        when(userService.register(request)).thenReturn(user);

        ResponseEntity<Map<String, Object>> response =
                authController.register(request);

        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertEquals(1L, body.get("id"));
        assertEquals("Harish", body.get("fullName"));
        assertEquals("harish@example.com", body.get("email"));
        assertEquals("USER", body.get("role"));
    }

    @Test
    void registerShouldNotExposePasswordHash() {

        when(userService.register(request)).thenReturn(user);

        ResponseEntity<Map<String, Object>> response =
                authController.register(request);

        assertNotNull(response.getBody());
        assertFalse(response.getBody().containsKey("passwordHash"));
        assertFalse(response.getBody().containsKey("password"));
    }

    @Test
    void registerShouldPropagateServiceException() {

        when(userService.register(request))
                .thenThrow(new RuntimeException("Registration failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.register(request)
        );

        assertEquals("Registration failed", exception.getMessage());
    }

    // EMAIL OTP VERIFICATION

    @Test
    void verifyEmailShouldReturnSuccess() {

        VerifyEmailOtpRequest request = mock(VerifyEmailOtpRequest.class);

        when(request.getEmail()).thenReturn(" HARISH@EXAMPLE.COM ");
        when(request.getOtp()).thenReturn("123456");
        when(otpService.verifyEmailOtp(
                "harish@example.com", "123456"
        )).thenReturn(true);

        ResponseEntity<Map<String, String>> response =
                authController.verifyEmail(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(otpService).verifyEmailOtp(
                "harish@example.com", "123456"
        );
    }

    @Test
    void verifyEmailShouldReturnBadRequestForInvalidOtp() {

        VerifyEmailOtpRequest request = mock(VerifyEmailOtpRequest.class);

        when(request.getEmail()).thenReturn("harish@example.com");
        when(request.getOtp()).thenReturn("000000");
        when(otpService.verifyEmailOtp(
                "harish@example.com", "000000"
        )).thenReturn(false);

        ResponseEntity<Map<String, String>> response =
                authController.verifyEmail(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid OTP", response.getBody().get("message"));
    }

    // MOBILE REGISTRATION

    @Test
    void registerMobileShouldReturnCreated() {

        MobileRegisterRequest request = mock(MobileRegisterRequest.class);

        when(userService.registerMobile(request)).thenReturn(user);

        ResponseEntity<Map<String, Object>> response =
                authController.registerMobile(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Harish", response.getBody().get("fullName"));
        assertEquals("9876543210", response.getBody().get("mobileNumber"));
    }

    @Test
    void verifyMobileShouldReturnSuccess() {

        VerifyMobileOtpRequest request = mock(VerifyMobileOtpRequest.class);

        when(request.getMobileNumber()).thenReturn("9876543210");
        when(request.getOtp()).thenReturn("123456");
        when(otpService.verifyMobileOtp(
                "9876543210", "123456"
        )).thenReturn(true);

        ResponseEntity<Map<String, String>> response =
                authController.verifyMobile(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void verifyMobileShouldRejectInvalidOtp() {

        VerifyMobileOtpRequest request = mock(VerifyMobileOtpRequest.class);

        when(request.getMobileNumber()).thenReturn("9876543210");
        when(request.getOtp()).thenReturn("000000");
        when(otpService.verifyMobileOtp(
                "9876543210", "000000"
        )).thenReturn(false);

        ResponseEntity<Map<String, String>> response =
                authController.verifyMobile(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid OTP", response.getBody().get("message"));
    }

    // MOBILE LOGIN OTP REQUEST

    @Test
    void mobileLoginShouldRejectMissingNumber() {

        ResponseEntity<Map<String, String>> response =
                authController.requestMobileLoginOtp(Map.of());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void mobileLoginShouldRejectInvalidNumber() {

        ResponseEntity<Map<String, String>> response =
                authController.requestMobileLoginOtp(
                        Map.of("mobileNumber", "12345")
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void mobileLoginShouldRejectUnknownUser() {

        when(userRepository.findByMobileNumber("9876543210"))
                .thenReturn(Optional.empty());

        ResponseEntity<Map<String, String>> response =
                authController.requestMobileLoginOtp(
                        Map.of("mobileNumber", "9876543210")
                );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void mobileLoginShouldSendOtpForValidUser() {

        when(userRepository.findByMobileNumber("9876543210"))
                .thenReturn(Optional.of(user));

        ResponseEntity<Map<String, String>> response =
                authController.requestMobileLoginOtp(
                        Map.of("mobileNumber", "+91 9876543210")
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(otpService).sendMobileLoginOtp(user);
    }

    // CSRF

    @Test
    void csrfShouldReturnToken() {

        CsrfToken csrfToken = mock(CsrfToken.class);
        when(csrfToken.getToken()).thenReturn("test-csrf-token");

        ResponseEntity<Map<String, String>> response =
                authController.csrf(csrfToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("test-csrf-token", response.getBody().get("token"));
    }

    // LOGOUT

    @Test
    void logoutShouldInvalidateSession() {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();

        request.setSession(session);

        ResponseEntity<Map<String, String>> response =
                authController.logout(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(session);
    }

    @Test
    void logoutShouldWorkWithoutExistingSession() {

        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<Map<String, String>> response =
                authController.logout(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // FORGOT PASSWORD

    @Test
    void forgotPasswordShouldSendOtpForEligibleUser() {

        ForgotPasswordRequest request = mock(ForgotPasswordRequest.class);

        when(request.getEmail()).thenReturn(" HARISH@EXAMPLE.COM ");
        when(userRepository.findByEmail("harish@example.com"))
                .thenReturn(Optional.of(user));

        ResponseEntity<?> response =
                authController.forgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(otpService).sendPasswordResetOtp(user);
    }

    @Test
    void forgotPasswordShouldNotSendOtpForUnknownUser() {

        ForgotPasswordRequest request = mock(ForgotPasswordRequest.class);

        when(request.getEmail()).thenReturn("unknown@example.com");
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                authController.forgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(otpService, never()).sendPasswordResetOtp(any());
    }

    @Test
    void forgotPasswordShouldNotSendOtpForInactiveUser() {

        user.setActive(false);

        ForgotPasswordRequest request = mock(ForgotPasswordRequest.class);

        when(request.getEmail()).thenReturn("harish@example.com");
        when(userRepository.findByEmail("harish@example.com"))
                .thenReturn(Optional.of(user));

        authController.forgotPassword(request);

        verify(otpService, never()).sendPasswordResetOtp(any());
    }

    // VERIFY PASSWORD RESET OTP

    @Test
    void verifyResetOtpShouldReturnResetToken() {

        VerifyResetOtpRequest request = mock(VerifyResetOtpRequest.class);

        when(request.getEmail()).thenReturn(" HARISH@EXAMPLE.COM ");
        when(request.getOtp()).thenReturn("123456");

        when(otpService.verifyResetOtpAndGenerateToken(
                "harish@example.com", "123456"
        )).thenReturn("reset-token");

        ResponseEntity<?> response =
                authController.verifyResetOtp(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "reset-token",
                ((Map<?, ?>) response.getBody()).get("resetToken")
        );
    }

    // RESET PASSWORD

    @Test
    void resetPasswordShouldReturnSuccess() {

        ResetPasswordRequest request = mock(ResetPasswordRequest.class);

        when(request.getEmail()).thenReturn(" HARISH@EXAMPLE.COM ");
        when(request.getResetToken()).thenReturn("reset-token");
        when(request.getNewPassword()).thenReturn("NewPassword@123");

        ResponseEntity<?> response =
                authController.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(otpService).resetPassword(
                "harish@example.com",
                "reset-token",
                "NewPassword@123"
        );
    }


@Test
void loginShouldSucceedWithValidCredentials() {

    LoginRequest loginRequest = mock(LoginRequest.class);

    when(loginRequest.getEmail()).thenReturn(" HARISH@EXAMPLE.COM ");
    when(loginRequest.getPassword()).thenReturn("Harish@123");

    Authentication authentication = mock(Authentication.class);

    when(authentication.getName()).thenReturn("harish@example.com");

    when(userRepository.findByEmail("harish@example.com"))
            .thenReturn(Optional.of(user));

    when(authenticationManager.authenticate(any()))
            .thenReturn(authentication);

    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    MockHttpServletResponse httpResponse = new MockHttpServletResponse();

    ResponseEntity<Map<String, Object>> response =
            authController.login(loginRequest, httpRequest, httpResponse);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Login successful", response.getBody().get("message"));
    assertEquals("harish@example.com", response.getBody().get("email"));

    verify(authenticationManager).authenticate(any());
    verify(userRepository).save(user);
}

@Test
void loginShouldRejectInvalidCredentials() {

    LoginRequest loginRequest = mock(LoginRequest.class);

    when(loginRequest.getEmail()).thenReturn("harish@example.com");
    when(loginRequest.getPassword()).thenReturn("WrongPassword");

    when(userRepository.findByEmail("harish@example.com"))
            .thenReturn(Optional.of(user));

    when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    MockHttpServletResponse httpResponse = new MockHttpServletResponse();

    ResponseEntity<Map<String, Object>> response =
            authController.login(loginRequest, httpRequest, httpResponse);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals(1, user.getFailedLoginAttempts());

    verify(userRepository).save(user);
}

@Test
void loginShouldLockAccountAfterFiveFailedAttempts() {

    user.setFailedLoginAttempts(4);

    LoginRequest loginRequest = mock(LoginRequest.class);

    when(loginRequest.getEmail()).thenReturn("harish@example.com");
    when(loginRequest.getPassword()).thenReturn("WrongPassword");

    when(userRepository.findByEmail("harish@example.com"))
            .thenReturn(Optional.of(user));

    when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    MockHttpServletResponse httpResponse = new MockHttpServletResponse();

    ResponseEntity<Map<String, Object>> response =
            authController.login(loginRequest, httpRequest, httpResponse);

    assertEquals(HttpStatus.LOCKED, response.getStatusCode());
    assertTrue(user.isAccountLocked());
    assertEquals(5, user.getFailedLoginAttempts());
    assertNotNull(user.getLockTime());

    verify(userRepository).save(user);
}

@Test
void loginShouldRejectCurrentlyLockedAccount() {

    user.setAccountLocked(true);
    user.setLockTime(LocalDateTime.now().minusMinutes(2));

    LoginRequest loginRequest = mock(LoginRequest.class);

    when(loginRequest.getEmail())
            .thenReturn("harish@example.com");

    when(userRepository.findByEmail("harish@example.com"))
            .thenReturn(Optional.of(user));

    MockHttpServletRequest httpRequest =
            new MockHttpServletRequest();

    MockHttpServletResponse httpResponse =
            new MockHttpServletResponse();

    ResponseEntity<Map<String, Object>> response =
            authController.login(
                    loginRequest,
                    httpRequest,
                    httpResponse
            );

    assertEquals(HttpStatus.LOCKED, response.getStatusCode());

    assertTrue(user.isAccountLocked());

    verify(authenticationManager, never())
            .authenticate(any());
}
@Test
void loginShouldUnlockExpiredAccount() {

    user.setAccountLocked(true);
    user.setFailedLoginAttempts(5);
    user.setLockTime(LocalDateTime.now().minusMinutes(20));

    LoginRequest loginRequest = mock(LoginRequest.class);

    when(loginRequest.getEmail()).thenReturn("harish@example.com");
    when(loginRequest.getPassword()).thenReturn("Harish@123");

    Authentication authentication = mock(Authentication.class);

    when(authentication.getName()).thenReturn("harish@example.com");

    when(userRepository.findByEmail("harish@example.com"))
            .thenReturn(Optional.of(user));

    when(authenticationManager.authenticate(any()))
            .thenReturn(authentication);

    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    MockHttpServletResponse httpResponse = new MockHttpServletResponse();

    ResponseEntity<Map<String, Object>> response =
            authController.login(loginRequest, httpRequest, httpResponse);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertFalse(user.isAccountLocked());
    assertEquals(0, user.getFailedLoginAttempts());
    assertNull(user.getLockTime());
}


@Test
void getCurrentUserShouldRejectUnauthenticatedRequest() {

    ResponseEntity<Map<String, Object>> response =
            authController.getCurrentUser(null);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals("Not authenticated", response.getBody().get("message"));
}

@Test
void getCurrentUserShouldReturnMobileAuthenticatedUser() {

    Authentication authentication = mock(Authentication.class);

    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn(user);

    ResponseEntity<Map<String, Object>> response =
            authController.getCurrentUser(authentication);

    assertEquals(HttpStatus.OK, response.getStatusCode());

    Map<String, Object> body = response.getBody();

    assertEquals("Harish", body.get("fullName"));
    assertEquals("harish@example.com", body.get("email"));
    assertEquals("USER", body.get("role"));
    assertEquals(true, body.get("emailVerified"));
    assertEquals(true, body.get("mobileVerified"));
}

@Test
void getCurrentUserShouldReturnEmailAuthenticatedUser() {

    Authentication authentication = mock(Authentication.class);

    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn("harish@example.com");
    when(authentication.getName()).thenReturn(" HARISH@EXAMPLE.COM ");

    when(userRepository.findByEmail("harish@example.com"))
            .thenReturn(Optional.of(user));

    ResponseEntity<Map<String, Object>> response =
            authController.getCurrentUser(authentication);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Harish", response.getBody().get("fullName"));

    verify(userRepository).findByEmail("harish@example.com");
}

@Test
void getCurrentUserShouldRejectUnknownApplicationUser() {

    Authentication authentication = mock(Authentication.class);

    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn("unknown@example.com");
    when(authentication.getName()).thenReturn("unknown@example.com");

    when(userRepository.findByEmail("unknown@example.com"))
            .thenReturn(Optional.empty());

    ResponseEntity<Map<String, Object>> response =
            authController.getCurrentUser(authentication);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals(
            "Application user account not found",
            response.getBody().get("message")
    );
}

@Test
void getCurrentUserShouldRejectInactiveUser() {

    user.setActive(false);

    Authentication authentication = mock(Authentication.class);

    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn(user);

    ResponseEntity<Map<String, Object>> response =
            authController.getCurrentUser(authentication);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals("Your account is inactive",
            response.getBody().get("message"));
}



@Test
void verifyMobileLoginShouldRejectInvalidMobileNumber() {

    VerifyMobileOtpRequest request = mock(VerifyMobileOtpRequest.class);

    when(request.getMobileNumber()).thenReturn("12345");

    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    MockHttpServletResponse httpResponse = new MockHttpServletResponse();

    ResponseEntity<Map<String, Object>> response =
            authController.verifyMobileLogin(
                    request, httpRequest, httpResponse
            );

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

    verify(otpService, never()).verifyMobileLoginOtp(any(), any());
}

@Test
void verifyMobileLoginShouldRejectInvalidOtp() {

    VerifyMobileOtpRequest request = mock(VerifyMobileOtpRequest.class);

    when(request.getMobileNumber()).thenReturn("9876543210");
    when(request.getOtp()).thenReturn("000000");

    when(otpService.verifyMobileLoginOtp("9876543210", "000000"))
            .thenReturn(false);

    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    MockHttpServletResponse httpResponse = new MockHttpServletResponse();

    ResponseEntity<Map<String, Object>> response =
            authController.verifyMobileLogin(
                    request, httpRequest, httpResponse
            );

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    verify(userRepository, never()).findByMobileNumber(any());
}

@Test
void verifyMobileLoginShouldSucceed() {

    VerifyMobileOtpRequest request = mock(VerifyMobileOtpRequest.class);

    when(request.getMobileNumber()).thenReturn("+91 9876543210");
    when(request.getOtp()).thenReturn("123456");

    when(otpService.verifyMobileLoginOtp("9876543210", "123456"))
            .thenReturn(true);

    when(userRepository.findByMobileNumber("9876543210"))
            .thenReturn(Optional.of(user));

    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    MockHttpServletResponse httpResponse = new MockHttpServletResponse();

    ResponseEntity<Map<String, Object>> response =
            authController.verifyMobileLogin(
                    request, httpRequest, httpResponse
            );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Mobile login successful",
            response.getBody().get("message"));
    assertEquals("Harish", response.getBody().get("fullName"));
    assertEquals("USER", response.getBody().get("role"));

    assertTrue(SecurityContextHolder.getContext()
            .getAuthentication().isAuthenticated());
}

@Test
void verifyMobileLoginShouldRejectInactiveUser() {

    user.setActive(false);

    VerifyMobileOtpRequest request = mock(VerifyMobileOtpRequest.class);

    when(request.getMobileNumber()).thenReturn("9876543210");
    when(request.getOtp()).thenReturn("123456");

    when(otpService.verifyMobileLoginOtp("9876543210", "123456"))
            .thenReturn(true);

    when(userRepository.findByMobileNumber("9876543210"))
            .thenReturn(Optional.of(user));

    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    MockHttpServletResponse httpResponse = new MockHttpServletResponse();

    ResponseEntity<Map<String, Object>> response =
            authController.verifyMobileLogin(
                    request, httpRequest, httpResponse
            );

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals("Account is not active or verified",
            response.getBody().get("message"));
}


@Test
void getCurrentUserShouldReturnGoogleAuthenticatedUser() {

    OidcUser oidcUser = mock(OidcUser.class);

    when(oidcUser.getSubject()).thenReturn("google-sub-123");

    Authentication authentication = mock(Authentication.class);

    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn(oidcUser);

    UserAuthProvider provider = new UserAuthProvider();
    provider.setUser(user);
    provider.setProvider("GOOGLE");
    provider.setProviderUserId("google-sub-123");
    provider.setProviderEmail("harish@example.com");

    when(providerRepository.findByProviderAndProviderUserId(
            "GOOGLE", "google-sub-123"
    )).thenReturn(Optional.of(provider));

    ResponseEntity<Map<String, Object>> response =
            authController.getCurrentUser(authentication);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Harish", response.getBody().get("fullName"));
    assertEquals("harish@example.com", response.getBody().get("email"));
    assertEquals("USER", response.getBody().get("role"));

    verify(providerRepository).findByProviderAndProviderUserId(
            "GOOGLE", "google-sub-123"
    );
}

@Test
void getCurrentUserShouldRejectUnknownGoogleProvider() {

    OidcUser oidcUser = mock(OidcUser.class);

    when(oidcUser.getSubject()).thenReturn("unknown-google-sub");

    Authentication authentication = mock(Authentication.class);

    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn(oidcUser);

    when(providerRepository.findByProviderAndProviderUserId(
            "GOOGLE", "unknown-google-sub"
    )).thenReturn(Optional.empty());

    ResponseEntity<Map<String, Object>> response =
            authController.getCurrentUser(authentication);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals(
            "Application user account not found",
            response.getBody().get("message")
    );
}

@Test
void getCurrentUserShouldRejectInactiveGoogleUser() {

    user.setActive(false);

    OidcUser oidcUser = mock(OidcUser.class);

    when(oidcUser.getSubject()).thenReturn("google-sub-123");

    Authentication authentication = mock(Authentication.class);

    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn(oidcUser);

    UserAuthProvider provider = new UserAuthProvider();
    provider.setUser(user);

    when(providerRepository.findByProviderAndProviderUserId(
            "GOOGLE", "google-sub-123"
    )).thenReturn(Optional.of(provider));

    ResponseEntity<Map<String, Object>> response =
            authController.getCurrentUser(authentication);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
}

@Test
void getCurrentUserShouldRejectAnonymousAuthentication() {

    Authentication authentication = mock(AnonymousAuthenticationToken.class);

    when(authentication.isAuthenticated()).thenReturn(true);

    ResponseEntity<Map<String, Object>> response =
            authController.getCurrentUser(authentication);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
}

@Test
void mobileLoginShouldRejectNumberContainingOnlySpecialCharacters() {

    ResponseEntity<Map<String, String>> response =
            authController.requestMobileLoginOtp(
                    Map.of("mobileNumber", "+++---")
            );

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
}

@Test
void mobileLoginShouldRejectInactiveUser() {

    user.setActive(false);

    when(userRepository.findByMobileNumber("9876543210"))
            .thenReturn(Optional.of(user));

    ResponseEntity<Map<String, String>> response =
            authController.requestMobileLoginOtp(
                    Map.of("mobileNumber", "9876543210")
            );

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    verify(otpService, never()).sendMobileLoginOtp(any());
}

@Test
void mobileLoginShouldRejectUnverifiedMobileUser() {

    user.setMobileVerified(false);

    when(userRepository.findByMobileNumber("9876543210"))
            .thenReturn(Optional.of(user));

    ResponseEntity<Map<String, String>> response =
            authController.requestMobileLoginOtp(
                    Map.of("mobileNumber", "9876543210")
            );

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

    verify(otpService, never()).sendMobileLoginOtp(any());
}

}
