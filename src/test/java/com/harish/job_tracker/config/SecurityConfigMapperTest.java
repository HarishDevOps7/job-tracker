
package com.harish.job_tracker.config;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserAuthProvider;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.repository.UserRepository;
import com.harish.job_tracker.repository.UserAuthProviderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigMapperTest {

    @Mock
    private UserAuthProviderRepository providerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthProvider provider;

    @Mock
    private User user;

    private org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SecurityConfig()
                .oauth2AuthoritiesMapper(providerRepository, userRepository);
    }

    private OidcUserAuthority createOidcAuthority() {
        Instant now = Instant.now();

        OidcIdToken token = new OidcIdToken(
                "test-token",
                now,
                now.plusSeconds(300),
                Map.of("sub", "google-user-123")
        );

        return new OidcUserAuthority(token);
    }

    @Test
    void mapperShouldPreserveNonOidcAuthorities() {
        GrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_USER");

        Collection<? extends GrantedAuthority> result =
                mapper.mapAuthorities(List.of(authority));

        assertTrue(result.contains(authority));
        verifyNoInteractions(providerRepository, userRepository);
    }

    @Test
    void mapperShouldRejectUnlinkedGoogleAccount() {
        when(providerRepository.findByProviderAndProviderUserId(
                "GOOGLE", "google-user-123"))
                .thenReturn(Optional.empty());

        assertThrows(
                OAuth2AuthenticationException.class,
                () -> mapper.mapAuthorities(
                        List.of(createOidcAuthority())
                )
        );
    }

    @Test
    void mapperShouldRejectMissingApplicationUser() {
        when(providerRepository.findByProviderAndProviderUserId(
                "GOOGLE", "google-user-123"))
                .thenReturn(Optional.of(provider));

        when(provider.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                OAuth2AuthenticationException.class,
                () -> mapper.mapAuthorities(
                        List.of(createOidcAuthority())
                )
        );
    }

    @Test
    void mapperShouldAddAdminRoleForLinkedUser() {
        when(providerRepository.findByProviderAndProviderUserId(
                "GOOGLE", "google-user-123"))
                .thenReturn(Optional.of(provider));

        when(provider.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(user.getRole()).thenReturn(UserRole.ADMIN);

        Collection<? extends GrantedAuthority> result =
                mapper.mapAuthorities(
                        List.of(createOidcAuthority())
                );

        assertTrue(result.contains(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        ));
    }
}
