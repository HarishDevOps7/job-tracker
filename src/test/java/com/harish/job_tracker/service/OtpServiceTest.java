
package com.harish.job_tracker.service;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.VerificationChallenge;
import com.harish.job_tracker.repository.UserRepository;
import com.harish.job_tracker.repository.VerificationChallengeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private VerificationChallengeRepository challengeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private OtpService otpService;

    private User user;

    @BeforeEach
void setUp() {
    ReflectionTestUtils.setField(otpService, "smsMode", "MOCK");

    user = mock(User.class);

    lenient().when(user.getId()).thenReturn(1L);
    lenient().when(user.getEmail()).thenReturn("test@example.com");
    lenient().when(user.getMobileNumber()).thenReturn("9876543210");
}

    private VerificationChallenge createChallenge(
            String channel,
            User associatedUser,
            String hash,
            int attempts,
            LocalDateTime expiry) {

        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setUser(associatedUser);
        challenge.setDestination(
                channel.equals("EMAIL") || channel.equals("PASSWORD_RESET")
                        ? "test@example.com"
                        : "9876543210"
        );
        challenge.setChannel(channel);
        challenge.setOtpHash(hash);
        challenge.setAttempts(attempts);
        challenge.setExpiresAt(expiry);

        return challenge;
    }

    private void mockChallenge(
            String destination,
            String channel,
            VerificationChallenge challenge) {

        when(challengeRepository
                .findTopByDestinationAndChannelAndVerifiedAtIsNullOrderByCreatedAtDesc(
                        destination, channel))
                .thenReturn(Optional.of(challenge));
    }

    private void assertStatus(
            HttpStatus expected,
            org.junit.jupiter.api.function.Executable action) {

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, action);

        assertEquals(expected, exception.getStatusCode());
    }

    // EMAIL OTP GENERATION

    @Test
    void sendEmailOtpSuccessfully() {
        when(challengeRepository
                .countByDestinationAndChannelAndCreatedAtAfter(
                        eq("test@example.com"),
                        eq("EMAIL"),
                        any(LocalDateTime.class)))
                .thenReturn(0L);

        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

        otpService.sendEmailOtp(user);

        ArgumentCaptor<VerificationChallenge> captor =
                ArgumentCaptor.forClass(VerificationChallenge.class);

        verify(challengeRepository).save(captor.capture());
        verify(emailService).sendOtp(eq("test@example.com"), anyString());

        VerificationChallenge saved = captor.getValue();

        assertEquals("EMAIL", saved.getChannel());
        assertEquals("hashed-otp", saved.getOtpHash());
        assertEquals(0, saved.getAttempts());
    }

    @Test
    void sendEmailOtpShouldRejectTooManyRequests() {
        when(challengeRepository
                .countByDestinationAndChannelAndCreatedAtAfter(
                        eq("test@example.com"),
                        eq("EMAIL"),
                        any(LocalDateTime.class)))
                .thenReturn(3L);

        assertStatus(
                HttpStatus.TOO_MANY_REQUESTS,
                () -> otpService.sendEmailOtp(user)
        );

        verify(emailService, never()).sendOtp(anyString(), anyString());
    }

    // EMAIL OTP VERIFICATION

    @Test
    void verifyEmailOtpSuccessfully() {
        VerificationChallenge challenge = createChallenge(
                "EMAIL", user, "hashed-otp", 0,
                LocalDateTime.now().plusMinutes(5)
        );

        mockChallenge("test@example.com", "EMAIL", challenge);

        when(passwordEncoder.matches("123456", "hashed-otp"))
                .thenReturn(true);

        assertTrue(otpService.verifyEmailOtp(
                "test@example.com", "123456"
        ));

        assertNotNull(challenge.getVerifiedAt());
        verify(user).setEmailVerified(true);
        verify(user).setActive(true);
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmailOtpShouldReturnFalseForInvalidOtp() {
        VerificationChallenge challenge = createChallenge(
                "EMAIL", user, "hashed-otp", 0,
                LocalDateTime.now().plusMinutes(5)
        );

        mockChallenge("test@example.com", "EMAIL", challenge);

        when(passwordEncoder.matches("111111", "hashed-otp"))
                .thenReturn(false);

        assertFalse(otpService.verifyEmailOtp(
                "test@example.com", "111111"
        ));

        assertEquals(1, challenge.getAttempts());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmailOtpShouldRejectMissingChallenge() {
        when(challengeRepository
                .findTopByDestinationAndChannelAndVerifiedAtIsNullOrderByCreatedAtDesc(
                        "test@example.com", "EMAIL"))
                .thenReturn(Optional.empty());

        assertStatus(
                HttpStatus.BAD_REQUEST,
                () -> otpService.verifyEmailOtp(
                        "test@example.com", "123456"
                )
        );
    }

    @Test
    void verifyEmailOtpShouldRejectExpiredOtp() {
        VerificationChallenge challenge = createChallenge(
                "EMAIL", user, "hashed-otp", 0,
                LocalDateTime.now().minusMinutes(1)
        );

        mockChallenge("test@example.com", "EMAIL", challenge);

        assertStatus(
                HttpStatus.BAD_REQUEST,
                () -> otpService.verifyEmailOtp(
                        "test@example.com", "123456"
                )
        );
    }

    @Test
    void verifyEmailOtpShouldRejectMaximumAttempts() {
        VerificationChallenge challenge = createChallenge(
                "EMAIL", user, "hashed-otp", 5,
                LocalDateTime.now().plusMinutes(5)
        );

        mockChallenge("test@example.com", "EMAIL", challenge);

        assertStatus(
                HttpStatus.TOO_MANY_REQUESTS,
                () -> otpService.verifyEmailOtp(
                        "test@example.com", "123456"
                )
        );
    }

    // MOBILE OTP GENERATION

    @Test
    void sendMobileOtpSuccessfullyInMockMode() {
        when(challengeRepository
                .countByDestinationAndChannelAndCreatedAtAfter(
                        eq("9876543210"),
                        eq("SMS"),
                        any(LocalDateTime.class)))
                .thenReturn(0L);

        when(userRepository.findByMobileNumber("9876543210"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString()))
                .thenReturn("hashed-mobile-otp");

        otpService.sendMobileOtp(user);

        verify(userRepository).save(user);

        ArgumentCaptor<VerificationChallenge> captor =
                ArgumentCaptor.forClass(VerificationChallenge.class);

        verify(challengeRepository).save(captor.capture());

        assertEquals("SMS", captor.getValue().getChannel());
        assertEquals("hashed-mobile-otp", captor.getValue().getOtpHash());
    }

    @Test
    void sendMobileOtpShouldRejectNullUser() {
        assertStatus(
                HttpStatus.BAD_REQUEST,
                () -> otpService.sendMobileOtp(null)
        );
    }

    @Test
    void sendMobileOtpShouldRejectInvalidMobile() {
        when(user.getMobileNumber()).thenReturn("12345");

        assertStatus(
                HttpStatus.BAD_REQUEST,
                () -> otpService.sendMobileOtp(user)
        );
    }

    @Test
    void sendMobileOtpShouldRejectDuplicateMobile() {
        User existingUser = mock(User.class);

        when(challengeRepository
                .countByDestinationAndChannelAndCreatedAtAfter(
                        eq("9876543210"),
                        eq("SMS"),
                        any(LocalDateTime.class)))
                .thenReturn(0L);

        when(userRepository.findByMobileNumber("9876543210"))
                .thenReturn(Optional.of(existingUser));

        when(existingUser.getId()).thenReturn(2L);

        assertStatus(
                HttpStatus.CONFLICT,
                () -> otpService.sendMobileOtp(user)
        );
    }

    // MOBILE OTP VERIFICATION

    @Test
    void verifyMobileOtpSuccessfully() {
        VerificationChallenge challenge = createChallenge(
                "SMS", user, "hashed-otp", 0,
                LocalDateTime.now().plusMinutes(5)
        );

        mockChallenge("9876543210", "SMS", challenge);

        when(passwordEncoder.matches("123456", "hashed-otp"))
                .thenReturn(true);

        assertTrue(otpService.verifyMobileOtp(
                "9876543210", "123456"
        ));

        assertNotNull(challenge.getVerifiedAt());
        verify(user).setMobileVerified(true);
        verify(user).setActive(true);
        verify(userRepository).save(user);
    }

    @Test
    void verifyMobileOtpShouldRejectInvalidFormat() {
        VerificationChallenge challenge = createChallenge(
                "SMS", user, "hashed-otp", 0,
                LocalDateTime.now().plusMinutes(5)
        );

        mockChallenge("9876543210", "SMS", challenge);

        assertFalse(otpService.verifyMobileOtp(
                "9876543210", "123"
        ));

        assertEquals(1, challenge.getAttempts());
    }

    @Test
    void verifyMobileOtpShouldRejectWrongOtp() {
        VerificationChallenge challenge = createChallenge(
                "SMS", user, "hashed-otp", 0,
                LocalDateTime.now().plusMinutes(5)
        );

        mockChallenge("9876543210", "SMS", challenge);

        when(passwordEncoder.matches("111111", "hashed-otp"))
                .thenReturn(false);

        assertFalse(otpService.verifyMobileOtp(
                "9876543210", "111111"
        ));

        assertEquals(1, challenge.getAttempts());
    }

    @Test
    void verifyMobileOtpShouldRejectExpiredOtp() {
        VerificationChallenge challenge = createChallenge(
                "SMS", user, "hashed-otp", 0,
                LocalDateTime.now().minusMinutes(1)
        );

        mockChallenge("9876543210", "SMS", challenge);

        assertStatus(
                HttpStatus.BAD_REQUEST,
                () -> otpService.verifyMobileOtp(
                        "9876543210", "123456"
                )
        );
    }

    // MOBILE LOGIN OTP

    @Test
    void sendMobileLoginOtpSuccessfully() {
        when(user.isActive()).thenReturn(true);
        when(user.isMobileVerified()).thenReturn(true);

        when(challengeRepository
                .countByDestinationAndChannelAndCreatedAtAfter(
                        eq("9876543210"),
                        eq("SMS_LOGIN"),
                        any(LocalDateTime.class)))
                .thenReturn(0L);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("hashed-login-otp");

        otpService.sendMobileLoginOtp(user);

        verify(challengeRepository).save(any(VerificationChallenge.class));
    }

    @Test
    void sendMobileLoginOtpShouldRejectInactiveUser() {
        when(user.isActive()).thenReturn(false);

        assertStatus(
                HttpStatus.UNAUTHORIZED,
                () -> otpService.sendMobileLoginOtp(user)
        );
    }

    @Test
    void verifyMobileLoginOtpSuccessfully() {
        VerificationChallenge challenge = createChallenge(
                "SMS_LOGIN", user, "hashed-otp", 0,
                LocalDateTime.now().plusMinutes(5)
        );

        mockChallenge("9876543210", "SMS_LOGIN", challenge);

        when(passwordEncoder.matches("123456", "hashed-otp"))
                .thenReturn(true);

        assertTrue(otpService.verifyMobileLoginOtp(
                "9876543210", "123456"
        ));

        assertNotNull(challenge.getVerifiedAt());

        verify(user, never()).setMobileVerified(true);
        verify(user, never()).setActive(true);
    }

    // PASSWORD RESET OTP

    @Test
    void sendPasswordResetOtpSuccessfully() {
        when(challengeRepository
                .countByDestinationAndChannelAndCreatedAtAfter(
                        eq("test@example.com"),
                        eq("PASSWORD_RESET"),
                        any(LocalDateTime.class)))
                .thenReturn(0L);

        when(passwordEncoder.encode(anyString()))
                .thenReturn("hashed-reset-otp");

        otpService.sendPasswordResetOtp(user);

        verify(emailService).sendOtp(
                eq("test@example.com"), anyString()
        );

        verify(challengeRepository).save(any(VerificationChallenge.class));
    }

    @Test
    void verifyResetOtpAndGenerateTokenSuccessfully() {
        VerificationChallenge challenge = createChallenge(
                "PASSWORD_RESET", user, "hashed-otp", 0,
                LocalDateTime.now().plusMinutes(5)
        );

        mockChallenge("test@example.com", "PASSWORD_RESET", challenge);

        when(passwordEncoder.matches("123456", "hashed-otp"))
                .thenReturn(true);

        String token = otpService.verifyResetOtpAndGenerateToken(
                "test@example.com", "123456"
        );

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertNotNull(challenge.getResetTokenHash());
        assertNotNull(challenge.getResetTokenExpiresAt());
    }

    @Test
    void verifyResetOtpShouldRejectInvalidOtp() {
        VerificationChallenge challenge = createChallenge(
                "PASSWORD_RESET", user, "hashed-otp", 0,
                LocalDateTime.now().plusMinutes(5)
        );

        mockChallenge("test@example.com", "PASSWORD_RESET", challenge);

        when(passwordEncoder.matches("111111", "hashed-otp"))
                .thenReturn(false);

        assertStatus(
                HttpStatus.BAD_REQUEST,
                () -> otpService.verifyResetOtpAndGenerateToken(
                        "test@example.com", "111111"
                )
        );

        assertEquals(1, challenge.getAttempts());
    }

   
@Test
void resetPasswordSuccessfully() {

    VerificationChallenge challenge = createChallenge(
            "PASSWORD_RESET",
            user,
            "hashed-otp",
            0,
            LocalDateTime.now().plusMinutes(5)
    );

    challenge.setVerifiedAt(LocalDateTime.now());

    challenge.setResetTokenHash(null);
    challenge.setResetTokenExpiresAt(null);

    when(challengeRepository
            .findTopByDestinationAndChannelAndVerifiedAtIsNullOrderByCreatedAtDesc(
                    "test@example.com", "PASSWORD_RESET"))
            .thenReturn(Optional.of(challenge));

    when(passwordEncoder.matches("123456", "hashed-otp"))
            .thenReturn(true);

    String token = otpService.verifyResetOtpAndGenerateToken(
            "test@example.com", "123456"
    );

    assertNotNull(token);

    when(challengeRepository
            .findTopByDestinationAndChannelAndVerifiedAtIsNotNullOrderByCreatedAtDesc(
                    "test@example.com", "PASSWORD_RESET"))
            .thenReturn(Optional.of(challenge));

    when(passwordEncoder.encode("NewPassword123"))
            .thenReturn("new-password-hash");

    otpService.resetPassword(
            "test@example.com",
            token,
            "NewPassword123"
    );

    verify(user).setPasswordHash("new-password-hash");
    verify(userRepository).save(user);

    assertNull(challenge.getResetTokenHash());
    assertNull(challenge.getResetTokenExpiresAt());

    verify(challengeRepository, times(2))
            .save(challenge);
}

    @Test
    void resetPasswordShouldRejectBlankToken() {
        assertStatus(
                HttpStatus.BAD_REQUEST,
                () -> otpService.resetPassword(
                        "test@example.com", " ", "NewPassword123"
                )
        );
    }


@Test
void sendMobileOtpSuccessfullyInMsg91Mode() {

    ReflectionTestUtils.setField(otpService, "smsMode", "MSG91");

    when(challengeRepository
            .countByDestinationAndChannelAndCreatedAtAfter(
                    eq("9876543210"),
                    eq("SMS"),
                    any(LocalDateTime.class)))
            .thenReturn(0L);

    when(userRepository.findByMobileNumber("9876543210"))
            .thenReturn(Optional.empty());

    otpService.sendMobileOtp(user);

    verify(smsService).sendOtp("9876543210");

    ArgumentCaptor<VerificationChallenge> captor =
            ArgumentCaptor.forClass(VerificationChallenge.class);

    verify(challengeRepository).save(captor.capture());

    VerificationChallenge saved = captor.getValue();

    assertEquals("SMS", saved.getChannel());
    assertNull(saved.getOtpHash());
}


@Test
void verifyMobileOtpSuccessfullyInMsg91Mode() {

    ReflectionTestUtils.setField(otpService, "smsMode", "MSG91");

    VerificationChallenge challenge = createChallenge(
            "SMS",
            user,
            null,
            0,
            LocalDateTime.now().plusMinutes(5)
    );

    mockChallenge("9876543210", "SMS", challenge);

    when(smsService.verifyOtp("9876543210", "123456"))
            .thenReturn(true);

    assertTrue(otpService.verifyMobileOtp(
            "9876543210",
            "123456"
    ));

    assertNotNull(challenge.getVerifiedAt());

    verify(smsService).verifyOtp("9876543210", "123456");
    verify(user).setMobileVerified(true);
    verify(user).setActive(true);
    verify(userRepository).save(user);
}


@Test
void sendMobileLoginOtpSuccessfullyInMsg91Mode() {
    ReflectionTestUtils.setField(otpService, "smsMode", "MSG91");

    when(user.isActive()).thenReturn(true);
    when(user.isMobileVerified()).thenReturn(true);

    when(challengeRepository
            .countByDestinationAndChannelAndCreatedAtAfter(
                    eq("9876543210"),
                    eq("SMS_LOGIN"),
                    any(LocalDateTime.class)))
            .thenReturn(0L);

    otpService.sendMobileLoginOtp(user);

    verify(smsService).sendOtp("9876543210");

    ArgumentCaptor<VerificationChallenge> captor =
            ArgumentCaptor.forClass(VerificationChallenge.class);

    verify(challengeRepository).save(captor.capture());

    assertEquals("SMS_LOGIN", captor.getValue().getChannel());
    assertNull(captor.getValue().getOtpHash());
}

@Test
void sendMobileLoginOtpShouldRejectTooManyRequests() {
    when(user.isActive()).thenReturn(true);
    when(user.isMobileVerified()).thenReturn(true);

    when(challengeRepository
            .countByDestinationAndChannelAndCreatedAtAfter(
                    eq("9876543210"),
                    eq("SMS_LOGIN"),
                    any(LocalDateTime.class)))
            .thenReturn(3L);

    assertStatus(
            HttpStatus.TOO_MANY_REQUESTS,
            () -> otpService.sendMobileLoginOtp(user)
    );

    verify(challengeRepository, never())
            .save(any(VerificationChallenge.class));
}

@Test
void sendMobileLoginOtpShouldRejectUnverifiedMobile() {
    when(user.isActive()).thenReturn(true);
    when(user.isMobileVerified()).thenReturn(false);

    assertStatus(
            HttpStatus.UNAUTHORIZED,
            () -> otpService.sendMobileLoginOtp(user)
    );

    verify(challengeRepository, never())
            .save(any(VerificationChallenge.class));
}


@Test
void verifyMobileLoginOtpSuccessfullyInMsg91Mode() {
    ReflectionTestUtils.setField(otpService, "smsMode", "MSG91");

    VerificationChallenge challenge = createChallenge(
            "SMS_LOGIN",
            user,
            null,
            0,
            LocalDateTime.now().plusMinutes(5)
    );

    mockChallenge("9876543210", "SMS_LOGIN", challenge);

    when(smsService.verifyOtp("9876543210", "123456"))
            .thenReturn(true);

    assertTrue(otpService.verifyMobileLoginOtp(
            "9876543210", "123456"
    ));

    assertNotNull(challenge.getVerifiedAt());
    verify(smsService).verifyOtp("9876543210", "123456");
}

@Test
void verifyMobileLoginOtpShouldRejectInvalidFormat() {
    VerificationChallenge challenge = createChallenge(
            "SMS_LOGIN", user, "hashed-otp", 0,
            LocalDateTime.now().plusMinutes(5)
    );

    mockChallenge("9876543210", "SMS_LOGIN", challenge);

    assertFalse(otpService.verifyMobileLoginOtp(
            "9876543210", "123"
    ));

    assertEquals(1, challenge.getAttempts());
    verify(challengeRepository).save(challenge);
}

@Test
void verifyMobileLoginOtpShouldRejectWrongOtp() {
    VerificationChallenge challenge = createChallenge(
            "SMS_LOGIN", user, "hashed-otp", 0,
            LocalDateTime.now().plusMinutes(5)
    );

    mockChallenge("9876543210", "SMS_LOGIN", challenge);

    when(passwordEncoder.matches("111111", "hashed-otp"))
            .thenReturn(false);

    assertFalse(otpService.verifyMobileLoginOtp(
            "9876543210", "111111"
    ));

    assertEquals(1, challenge.getAttempts());
}

@Test
void verifyMobileLoginOtpShouldRejectExpiredOtp() {
    VerificationChallenge challenge = createChallenge(
            "SMS_LOGIN", user, "hashed-otp", 0,
            LocalDateTime.now().minusMinutes(1)
    );

    mockChallenge("9876543210", "SMS_LOGIN", challenge);

    assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> otpService.verifyMobileLoginOtp(
                    "9876543210", "123456"
            )
    );
}


@Test
void verifyMobileLoginOtpShouldRejectMaximumAttempts() {
    VerificationChallenge challenge = createChallenge(
            "SMS_LOGIN", user, "hashed-otp", 5,
            LocalDateTime.now().plusMinutes(5)
    );

    mockChallenge("9876543210", "SMS_LOGIN", challenge);

    assertStatus(
            HttpStatus.TOO_MANY_REQUESTS,
            () -> otpService.verifyMobileLoginOtp(
                    "9876543210", "123456"
            )
    );
}

@Test
void verifyMobileLoginOtpShouldRejectMissingChallenge() {
    when(challengeRepository
            .findTopByDestinationAndChannelAndVerifiedAtIsNullOrderByCreatedAtDesc(
                    "9876543210", "SMS_LOGIN"))
            .thenReturn(Optional.empty());

    assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> otpService.verifyMobileLoginOtp(
                    "9876543210", "123456"
            )
    );
}

@Test
void resetPasswordShouldRejectMissingChallenge() {
    when(challengeRepository
            .findTopByDestinationAndChannelAndVerifiedAtIsNotNullOrderByCreatedAtDesc(
                    "test@example.com", "PASSWORD_RESET"))
            .thenReturn(Optional.empty());

    assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> otpService.resetPassword(
                    "test@example.com", "valid-token", "NewPassword123"
            )
    );
}

@Test
void resetPasswordShouldRejectInvalidToken() {
    VerificationChallenge challenge = createChallenge(
            "PASSWORD_RESET", user, "hashed-otp", 0,
            LocalDateTime.now().plusMinutes(5)
    );

    challenge.setVerifiedAt(LocalDateTime.now());
    challenge.setResetTokenHash("incorrect-stored-hash");
    challenge.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(5));

    when(challengeRepository
            .findTopByDestinationAndChannelAndVerifiedAtIsNotNullOrderByCreatedAtDesc(
                    "test@example.com", "PASSWORD_RESET"))
            .thenReturn(Optional.of(challenge));

    assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> otpService.resetPassword(
                    "test@example.com", "wrong-token", "NewPassword123"
            )
    );
}

@Test
void resetPasswordShouldRejectExpiredToken() {
    VerificationChallenge challenge = createChallenge(
            "PASSWORD_RESET", user, "hashed-otp", 0,
            LocalDateTime.now().plusMinutes(5)
    );

    challenge.setVerifiedAt(LocalDateTime.now());
    challenge.setResetTokenHash("some-hash");
    challenge.setResetTokenExpiresAt(LocalDateTime.now().minusMinutes(1));

    when(challengeRepository
            .findTopByDestinationAndChannelAndVerifiedAtIsNotNullOrderByCreatedAtDesc(
                    "test@example.com", "PASSWORD_RESET"))
            .thenReturn(Optional.of(challenge));

    assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> otpService.resetPassword(
                    "test@example.com", "valid-token", "NewPassword123"
            )
    );
}


@Test
void resetPasswordShouldRejectMissingUser() throws Exception {
    VerificationChallenge challenge = createChallenge(
            "PASSWORD_RESET", null, "hashed-otp", 0,
            LocalDateTime.now().plusMinutes(5)
    );

    challenge.setVerifiedAt(LocalDateTime.now());

    String token = "test-reset-token";

    java.security.MessageDigest digest =
            java.security.MessageDigest.getInstance("SHA-256");

    String tokenHash = java.util.HexFormat.of().formatHex(
            digest.digest(token.getBytes(
                    java.nio.charset.StandardCharsets.UTF_8))
    );

    challenge.setResetTokenHash(tokenHash);
    challenge.setResetTokenExpiresAt(
            LocalDateTime.now().plusMinutes(5)
    );

    when(challengeRepository
            .findTopByDestinationAndChannelAndVerifiedAtIsNotNullOrderByCreatedAtDesc(
                    "test@example.com", "PASSWORD_RESET"))
            .thenReturn(Optional.of(challenge));

    assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> otpService.resetPassword(
                    "test@example.com", token, "NewPassword123"
            )
    );

    verify(userRepository, never()).save(any());
}


@Test
void verifyMobileOtpShouldRejectMissingChallenge() {
    when(challengeRepository
            .findTopByDestinationAndChannelAndVerifiedAtIsNullOrderByCreatedAtDesc(
                    "9876543210", "SMS"))
            .thenReturn(Optional.empty());

    assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> otpService.verifyMobileOtp("9876543210", "123456")
    );
}

@Test
void verifyMobileOtpShouldRejectMaximumAttempts() {
    VerificationChallenge challenge = createChallenge(
            "SMS", user, "hashed-otp", 5,
            LocalDateTime.now().plusMinutes(5)
    );

    mockChallenge("9876543210", "SMS", challenge);

    assertStatus(
            HttpStatus.TOO_MANY_REQUESTS,
            () -> otpService.verifyMobileOtp("9876543210", "123456")
    );
}

@Test
void verifyMobileOtpShouldRejectMissingAssociatedUser() {
    VerificationChallenge challenge = createChallenge(
            "SMS", null, "hashed-otp", 0,
            LocalDateTime.now().plusMinutes(5)
    );

    mockChallenge("9876543210", "SMS", challenge);

    when(passwordEncoder.matches("123456", "hashed-otp"))
            .thenReturn(true);

    assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> otpService.verifyMobileOtp("9876543210", "123456")
    );
}


@Test
void verifyMobileOtpShouldRejectWrongOtpInMsg91Mode() {
    ReflectionTestUtils.setField(otpService, "smsMode", "MSG91");

    VerificationChallenge challenge = createChallenge(
            "SMS", user, null, 0,
            LocalDateTime.now().plusMinutes(5)
    );

    mockChallenge("9876543210", "SMS", challenge);

    when(smsService.verifyOtp("9876543210", "123456"))
            .thenReturn(false);

    assertFalse(otpService.verifyMobileOtp(
            "9876543210", "123456"
    ));

    assertEquals(1, challenge.getAttempts());
    verify(challengeRepository).save(challenge);
}

@Test
void verifyMobileOtpShouldRejectNullHashInMockMode() {
    VerificationChallenge challenge = createChallenge(
            "SMS", user, null, 0,
            LocalDateTime.now().plusMinutes(5)
    );

    mockChallenge("9876543210", "SMS", challenge);

    assertFalse(otpService.verifyMobileOtp(
            "9876543210", "123456"
    ));

    assertEquals(1, challenge.getAttempts());
    verify(passwordEncoder, never())
            .matches(anyString(), anyString());
}

@Test
void sendMobileOtpShouldNormalizeIndianCountryCode() {
    when(user.getMobileNumber()).thenReturn("+91 9876543210");

    when(challengeRepository
            .countByDestinationAndChannelAndCreatedAtAfter(
                    eq("9876543210"), eq("SMS"), any(LocalDateTime.class)))
            .thenReturn(0L);

    when(userRepository.findByMobileNumber("9876543210"))
            .thenReturn(Optional.empty());

    when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

    otpService.sendMobileOtp(user);

    ArgumentCaptor<VerificationChallenge> captor =
            ArgumentCaptor.forClass(VerificationChallenge.class);

    verify(challengeRepository).save(captor.capture());
    assertEquals("9876543210", captor.getValue().getDestination());
}

@Test
void sendMobileOtpShouldRejectNullMobileNumber() {
    when(user.getMobileNumber()).thenReturn(null);

    assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> otpService.sendMobileOtp(user)
    );
}

@Test
void sendMobileOtpShouldRejectUserWithoutId() {
    when(user.getId()).thenReturn(null);

    assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> otpService.sendMobileOtp(user)
    );
}

@Test
void sendMobileOtpShouldRejectTooManyRequests() {
    when(challengeRepository
            .countByDestinationAndChannelAndCreatedAtAfter(
                    eq("9876543210"), eq("SMS"), any(LocalDateTime.class)))
            .thenReturn(3L);

    assertStatus(
            HttpStatus.TOO_MANY_REQUESTS,
            () -> otpService.sendMobileOtp(user)
    );

    verify(userRepository, never()).save(any());
    verify(challengeRepository, never()).save(any());
}

@Test
void sendMobileOtpShouldAllowExistingMobileForSameUser() {
    User existingUser = mock(User.class);

    when(challengeRepository
            .countByDestinationAndChannelAndCreatedAtAfter(
                    eq("9876543210"), eq("SMS"), any(LocalDateTime.class)))
            .thenReturn(0L);

    when(userRepository.findByMobileNumber("9876543210"))
            .thenReturn(Optional.of(existingUser));

    when(existingUser.getId()).thenReturn(1L);
    when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");

    otpService.sendMobileOtp(user);

    verify(userRepository).save(user);
    verify(challengeRepository).save(any(VerificationChallenge.class));
}

@Test
void sendMobileOtpShouldRejectUnsupportedSmsMode() {
    ReflectionTestUtils.setField(otpService, "smsMode", "TWILIO");

    when(challengeRepository
            .countByDestinationAndChannelAndCreatedAtAfter(
                    eq("9876543210"), eq("SMS"), any(LocalDateTime.class)))
            .thenReturn(0L);

    when(userRepository.findByMobileNumber("9876543210"))
            .thenReturn(Optional.empty());

    assertThrows(
            IllegalStateException.class,
            () -> otpService.sendMobileOtp(user)
    );

    verify(challengeRepository, never()).save(any());
}

@Test
void verifyResetOtpShouldRejectExpiredOtp() {
    VerificationChallenge challenge = createChallenge(
            "PASSWORD_RESET", user, "hashed-otp", 0,
            LocalDateTime.now().minusMinutes(1)
    );

    mockChallenge("test@example.com", "PASSWORD_RESET", challenge);

    assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> otpService.verifyResetOtpAndGenerateToken(
                    "test@example.com", "123456")
    );
}

@Test
void verifyResetOtpShouldRejectMaximumAttempts() {
    VerificationChallenge challenge = createChallenge(
            "PASSWORD_RESET", user, "hashed-otp", 5,
            LocalDateTime.now().plusMinutes(5)
    );

    mockChallenge("test@example.com", "PASSWORD_RESET", challenge);

    assertStatus(
            HttpStatus.TOO_MANY_REQUESTS,
            () -> otpService.verifyResetOtpAndGenerateToken(
                    "test@example.com", "123456")
    );
}

@Test
void verifyResetOtpShouldRejectNullOtpHash() {
    VerificationChallenge challenge = createChallenge(
            "PASSWORD_RESET", user, null, 0,
            LocalDateTime.now().plusMinutes(5)
    );

    mockChallenge("test@example.com", "PASSWORD_RESET", challenge);

    assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> otpService.verifyResetOtpAndGenerateToken(
                    "test@example.com", "123456")
    );

    assertEquals(1, challenge.getAttempts());
}


}
