
package com.harish.job_tracker.service;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserAuthProvider;
import com.harish.job_tracker.repository.UserRepository;
import com.harish.job_tracker.repository.UserAuthProviderRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Locale;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository providerRepository;

    public CurrentUserService(
            UserRepository userRepository,
            UserAuthProviderRepository providerRepository) {

        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
    }

    public User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User is not authenticated"
            );
        }

        Object principal = authentication.getPrincipal();

        User user = null;

        // Mobile OTP authentication
        if (principal instanceof User authenticatedUser) {
            user = userRepository.findById(authenticatedUser.getId())
                    .orElse(null);
        }

        // Google OAuth authentication
        else if (principal instanceof OidcUser oidcUser) {

            String googleSubject = oidcUser.getSubject();

            user = providerRepository
                    .findByProviderAndProviderUserId(
                            "GOOGLE",
                            googleSubject
                    )
                    .map(UserAuthProvider::getUser)
                    .orElse(null);
        }

        // Email/password authentication
        else if (principal instanceof UserDetails userDetails) {

            String email = userDetails.getUsername()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            user = userRepository.findByEmail(email)
                    .orElse(null);
        }

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Application user not found"
            );
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User account is inactive"
            );
        }

        return user;
    }
}
