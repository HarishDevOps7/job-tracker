
package com.harish.job_tracker.service;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserAuthProvider;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.repository.UserAuthProviderRepository;
import com.harish.job_tracker.repository.UserRepository;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository providerRepository;

    public GoogleOAuth2UserService(
            UserRepository userRepository,
            UserAuthProviderRepository providerRepository) {
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest)
            throws OAuth2AuthenticationException {

        OidcUser oidcUser = super.loadUser(userRequest);

        String provider = "GOOGLE";
        String providerUserId = oidcUser.getSubject();
        String email = oidcUser.getEmail();

        Boolean emailVerified =
                oidcUser.getClaimAsBoolean("email_verified");

        if (email == null || email.isBlank()
                || !Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_google_account"),
                    "Google account must have a verified email address."
            );
        }

        email = email.trim().toLowerCase();

        // 1. Check whether this Google account is already linked.
        var existingProvider =
                providerRepository.findByProviderAndProviderUserId(
                        provider, providerUserId
                );

        if (existingProvider.isPresent()) {
            return oidcUser;
        }

        // 2. Do not automatically link an existing email account.
        if (userRepository.findByEmail(email).isPresent()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_already_exists"),
                    "An account already exists with this email. "
                    + "Sign in using your existing method and link Google."
            );
        }

        // 3. Create a new application user.
        User user = new User();

        String fullName = oidcUser.getFullName();

        user.setFullName(
                fullName != null && !fullName.isBlank()
                        ? fullName
                        : email
        );

        user.setEmail(email);
        user.setPasswordHash(null);
        user.setEmailVerified(true);
        user.setActive(true);
        user.setRole(UserRole.USER);

        User savedUser = userRepository.save(user);

        // 4. Save the Google provider identity.
        UserAuthProvider authProvider = new UserAuthProvider();

        authProvider.setUser(savedUser);
        authProvider.setProvider(provider);
        authProvider.setProviderUserId(providerUserId);
        authProvider.setProviderEmail(email);

        providerRepository.save(authProvider);

        return oidcUser;
    }
}
