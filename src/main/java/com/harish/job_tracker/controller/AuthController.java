
package com.harish.job_tracker.controller;

import com.harish.job_tracker.dto.LoginRequest;
import com.harish.job_tracker.dto.RegisterRequest;
import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.repository.UserRepository;
import com.harish.job_tracker.service.UserService;

import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(
            UserService userService,
            UserRepository userRepository,
            AuthenticationManager authenticationManager) {

        this.userService = userService;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
    }

    // Registration API
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = userService.register(request);

        Map<String, Object> response = Map.of(
        "message", "Registration successful",
        "id", user.getId(),
        "fullName", user.getFullName(),
        "email", user.getEmail(),
        "role", user.getRole()
);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Login API
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {

            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.getEmail().trim().toLowerCase(),
                                    request.getPassword()
                            )
                    );

            User user = userRepository
                    .findByEmail(authentication.getName())
                    .orElseThrow();

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(
                    context,
                    httpRequest,
                    httpResponse
            );

            Map<String, Object> response = Map.of(
                    "message", "Login successful",
                    "id", user.getId(),
                    "fullName", user.getFullName(),
                    "email", user.getEmail(),
                    "role", user.getRole().name()
            );

            return ResponseEntity.ok(response);

        } catch (AuthenticationException exception) {

            SecurityContextHolder.clearContext();

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Invalid email or password"
                    ));
        }
    }

    @GetMapping("/csrf")
public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {

    return ResponseEntity.ok(
            Map.of("token", csrfToken.getToken())
    );
}
@GetMapping("/me")
public ResponseEntity<Map<String, Object>> getCurrentUser(
        Authentication authentication) {

    if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Not authenticated"));
    }

    User user = userRepository
            .findByEmail(authentication.getName())
            .orElseThrow();

    Map<String, Object> response = Map.of(
            "id", user.getId(),
            "fullName", user.getFullName(),
            "email", user.getEmail(),
            "role", user.getRole().name()
    );

    return ResponseEntity.ok(response);
}


@PostMapping("/logout")
public ResponseEntity<Map<String, String>> logout(
        HttpServletRequest request) {

    var session = request.getSession(false);

    if (session != null) {
        session.invalidate();
    }

    SecurityContextHolder.clearContext();

    return ResponseEntity.ok(
            Map.of("message", "Logout successful")
    );
}


}
