
package com.harish.job_tracker.service;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserAuthProvider;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.repository.UserAuthProviderRepository;
import com.harish.job_tracker.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthProviderRepository providerRepository;

    @Mock
    private OidcUser oidcUser;

    private GoogleOAuth2UserService googleService;

    @BeforeEach
    void setUp() {
        googleService = new GoogleOAuth2UserService(
                userRepository, providerRepository
        );
    }

    
private void mockGoogleUser(
        String email, Boolean verified, String name) {

    lenient().when(oidcUser.getSubject())
            .thenReturn("google-123");

    lenient().when(oidcUser.getEmail())
            .thenReturn(email);

    lenient().when(oidcUser.getClaimAsBoolean("email_verified"))
            .thenReturn(verified);

    lenient().when(oidcUser.getFullName())
            .thenReturn(name);
}


    @Test
    void shouldCreateNewGoogleUser() {
        mockGoogleUser("harish@example.com", true, "Harish");

        when(providerRepository.findByProviderAndProviderUserId(
                "GOOGLE", "google-123"))
                .thenReturn(Optional.empty());

        when(userRepository.findByEmail("harish@example.com"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OidcUser result = googleService.processOidcUser(oidcUser);

        assertSame(oidcUser, result);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("Harish", savedUser.getFullName());
        assertEquals("harish@example.com", savedUser.getEmail());
        assertNull(savedUser.getPasswordHash());
        assertTrue(savedUser.isEmailVerified());
        assertTrue(savedUser.isActive());
        assertEquals(UserRole.USER, savedUser.getRole());

        ArgumentCaptor<UserAuthProvider> providerCaptor =
                ArgumentCaptor.forClass(UserAuthProvider.class);

        verify(providerRepository).save(providerCaptor.capture());

        UserAuthProvider savedProvider = providerCaptor.getValue();

        assertSame(savedUser, savedProvider.getUser());
        assertEquals("GOOGLE", savedProvider.getProvider());
        assertEquals("google-123", savedProvider.getProviderUserId());
        assertEquals("harish@example.com",
                savedProvider.getProviderEmail());
    }

    @Test
    void shouldNormalizeEmailBeforeCreatingUser() {
        mockGoogleUser("  HARISH@EXAMPLE.COM  ", true, "Harish");

        when(providerRepository.findByProviderAndProviderUserId(
                "GOOGLE", "google-123"))
                .thenReturn(Optional.empty());

        when(userRepository.findByEmail("harish@example.com"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        googleService.processOidcUser(oidcUser);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        assertEquals("harish@example.com", captor.getValue().getEmail());
    }

    @Test
    void shouldUseEmailAsNameWhenGoogleNameIsBlank() {
        mockGoogleUser("harish@example.com", true, " ");

        when(providerRepository.findByProviderAndProviderUserId(
                "GOOGLE", "google-123"))
                .thenReturn(Optional.empty());

        when(userRepository.findByEmail("harish@example.com"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        googleService.processOidcUser(oidcUser);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        assertEquals("harish@example.com",
                captor.getValue().getFullName());
    }

    @Test
    void shouldReturnExistingGoogleAccount() {
        mockGoogleUser("harish@example.com", true, "Harish");

        when(providerRepository.findByProviderAndProviderUserId(
                "GOOGLE", "google-123"))
                .thenReturn(Optional.of(new UserAuthProvider()));

        OidcUser result = googleService.processOidcUser(oidcUser);

        assertSame(oidcUser, result);

        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(providerRepository, never()).save(any(UserAuthProvider.class));
    }

    @Test
    void shouldRejectExistingEmailAccount() {
        mockGoogleUser("harish@example.com", true, "Harish");

        when(providerRepository.findByProviderAndProviderUserId(
                "GOOGLE", "google-123"))
                .thenReturn(Optional.empty());

        when(userRepository.findByEmail("harish@example.com"))
                .thenReturn(Optional.of(new User()));

        assertThrows(
                OAuth2AuthenticationException.class,
                () -> googleService.processOidcUser(oidcUser)
        );

        verify(userRepository, never()).save(any(User.class));
        verify(providerRepository, never()).save(any(UserAuthProvider.class));
    }

    @Test
    void shouldRejectUnverifiedEmail() {
        mockGoogleUser("harish@example.com", false, "Harish");

        assertThrows(
                OAuth2AuthenticationException.class,
                () -> googleService.processOidcUser(oidcUser)
        );

        verifyNoInteractions(userRepository, providerRepository);
    }

    @Test
    void shouldRejectNullEmail() {
        mockGoogleUser(null, true, "Harish");

        assertThrows(
                OAuth2AuthenticationException.class,
                () -> googleService.processOidcUser(oidcUser)
        );

        verifyNoInteractions(userRepository, providerRepository);
    }

    @Test
    void shouldRejectBlankEmail() {
        mockGoogleUser("   ", true, "Harish");

        assertThrows(
                OAuth2AuthenticationException.class,
                () -> googleService.processOidcUser(oidcUser)
        );

        verifyNoInteractions(userRepository, providerRepository);
    }

    @Test
    void shouldRejectNullEmailVerificationClaim() {
        mockGoogleUser("harish@example.com", null, "Harish");

        assertThrows(
                OAuth2AuthenticationException.class,
                () -> googleService.processOidcUser(oidcUser)
        );

        verifyNoInteractions(userRepository, providerRepository);
    }
}
