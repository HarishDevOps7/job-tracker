
package com.harish.job_tracker.controller;
import java.time.LocalDateTime;
import com.harish.job_tracker.dto.LoginRequest;
import com.harish.job_tracker.dto.RegisterRequest;
import com.harish.job_tracker.dto.VerifyEmailOtpRequest;
import com.harish.job_tracker.dto.MobileRegisterRequest;
import com.harish.job_tracker.dto.VerifyMobileOtpRequest;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.repository.UserRepository;
import com.harish.job_tracker.service.UserService;
import com.harish.job_tracker.service.OtpService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.harish.job_tracker.entity.UserAuthProvider;
import com.harish.job_tracker.repository.UserAuthProviderRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import org.springframework.security.web.csrf.CsrfToken;

import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;

import com.harish.job_tracker.dto.ForgotPasswordRequest;
import com.harish.job_tracker.dto.VerifyResetOtpRequest;
import com.harish.job_tracker.dto.ResetPasswordRequest;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    @Autowired
private UserAuthProviderRepository userAuthProviderRepository;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(
            UserService userService,
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            OtpService otpService) {

        this.userService = userService;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.otpService = otpService;
    }

    // ==========================================
    // EMAIL REGISTRATION
    // ==========================================

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "message", "OTP sent to your registered email",
                        "id", user.getId(),
                        "fullName", user.getFullName(),
                        "email", user.getEmail(),
                        "role", user.getRole().name()
                )
        );
    }

    // ==========================================
    // EMAIL OTP VERIFICATION
    // ==========================================

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Valid @RequestBody VerifyEmailOtpRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        boolean verified = otpService.verifyEmailOtp(
                email,
                request.getOtp()
        );

        if (!verified) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid OTP")
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Email verified successfully. You can now log in."
                )
        );
    }

    // ==========================================
    // EMAIL AND PASSWORD LOGIN
    // ==========================================

@PostMapping("/login")
public ResponseEntity<Map<String, Object>> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse) {

    String email = request.getEmail()
            .trim()
            .toLowerCase(Locale.ROOT);

    User user = userRepository.findByEmail(email).orElse(null);

    // Check whether the account is currently locked.
    if (user != null && user.isAccountLocked()) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockTime = user.getLockTime();

        if (lockTime != null
                && now.isBefore(lockTime.plusMinutes(LOCK_DURATION_MINUTES))) {

            return ResponseEntity.status(HttpStatus.LOCKED).body(
                    Map.of("message",
                            "Account temporarily locked. Please try again later.")
            );
        }

        // Automatically unlock after 15 minutes.
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);
    }

    try {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                request.getPassword()
                        )
                );

        user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        // Reset failed attempts after successful login.
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);
        userRepository.save(user);

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "Login successful",
                        "id", user.getId(),
                        "fullName", user.getFullName(),
                        "email", user.getEmail(),
                        "role", user.getRole().name()
                )
        );

    } catch (AuthenticationException exception) {

        SecurityContextHolder.clearContext();

        // Track failed attempts for existing accounts.
        if (user != null) {

            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_LOGIN_ATTEMPTS) {

                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());

                userRepository.save(user);

                return ResponseEntity.status(HttpStatus.LOCKED).body(
                        Map.of("message",
                                "Account locked after 5 failed login attempts. Try again after 15 minutes.")
                );
            }

            userRepository.save(user);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("message", "Invalid email or password")
        );
    }
}

    
    // ==========================================
    // MOBILE REGISTRATION
    // ==========================================

    @PostMapping("/register/mobile")
    public ResponseEntity<Map<String, Object>> registerMobile(
            @Valid @RequestBody MobileRegisterRequest request) {

        User user = userService.registerMobile(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "message", "Mobile OTP sent successfully",
                        "id", user.getId(),
                        "fullName", user.getFullName(),
                        "mobileNumber", user.getMobileNumber(),
                        "role", user.getRole().name()
                )
        );
    }

    // ==========================================
    // MOBILE REGISTRATION OTP VERIFICATION
    // ==========================================

    @PostMapping("/verify-mobile")
    public ResponseEntity<Map<String, String>> verifyMobile(
            @Valid @RequestBody VerifyMobileOtpRequest request) {

        boolean verified = otpService.verifyMobileOtp(
                request.getMobileNumber(),
                request.getOtp()
        );

        if (!verified) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid OTP")
            );
        }

        return ResponseEntity.ok(
                Map.of("message", "Mobile number verified successfully")
        );
    }

    // ==========================================
    // MOBILE LOGIN - REQUEST OTP
    // ==========================================

    @PostMapping("/login/mobile")
    public ResponseEntity<Map<String, String>> requestMobileLoginOtp(
            @RequestBody Map<String, String> request) {

        String mobileNumber = request.get("mobileNumber");

        if (mobileNumber == null || mobileNumber.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Mobile number is required")
            );
        }

        String mobile = normalizeMobile(mobileNumber);

        if (mobile == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Enter a valid mobile number")
            );
        }

        User user = userRepository
                .findByMobileNumber(mobile)
                .orElse(null);

        if (user == null
                || !user.isActive()
                || !user.isMobileVerified()) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of(
                            "message",
                            "Mobile login is unavailable for this account"
                    )
            );
        }

        // Reuse existing OTP generation service.
        otpService.sendMobileLoginOtp(user);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Login OTP generated successfully"
                )
        );
    }

    // ==========================================
    // MOBILE LOGIN - VERIFY OTP AND CREATE SESSION
    // ==========================================

    @PostMapping("/verify-mobile-login")
    public ResponseEntity<Map<String, Object>> verifyMobileLogin(
            @Valid @RequestBody VerifyMobileOtpRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String mobile = normalizeMobile(request.getMobileNumber());

        if (mobile == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Enter a valid mobile number")
            );
        }

  boolean verified = otpService.verifyMobileLoginOtp(
        mobile,
        request.getOtp()
);

        if (!verified) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Invalid OTP")
            );
        }

        User user = userRepository
                .findByMobileNumber(mobile)
                .orElseThrow();

        if (!user.isActive() || !user.isMobileVerified()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Account is not active or verified")
            );
        }

        // Create authenticated principal with existing account role.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + user.getRole().name()
                                )
                        )
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        // Save authentication in HTTP session.
        securityContextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("message", "Mobile login successful");
        response.put("id", user.getId());
        response.put("fullName", user.getFullName());
        response.put("mobileNumber", user.getMobileNumber());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // MOBILE NUMBER NORMALIZATION
    // ==========================================

    private String normalizeMobile(String mobileNumber) {

        if (mobileNumber == null) {
            return null;
        }

        String mobile = mobileNumber.replaceAll("\\D", "");

        if (mobile.length() == 12 && mobile.startsWith("91")) {
            mobile = mobile.substring(2);
        }

        if (!mobile.matches("[6-9]\\d{9}")) {
            return null;
        }

        return mobile;
    }

    // ==========================================
    // CSRF TOKEN
    // ==========================================

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(
            CsrfToken csrfToken) {

        return ResponseEntity.ok(
                Map.of("token", csrfToken.getToken())
        );
    }

    // ==========================================
    // CURRENT AUTHENTICATED USER
    // ==========================================

  @GetMapping("/me")
public ResponseEntity<Map<String, Object>> getCurrentUser(
        Authentication authentication) {

    if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("message", "Not authenticated")
        );
    }

    User user = null;

    Object principal = authentication.getPrincipal();

    // 1. Mobile OTP authentication
    if (principal instanceof User authenticatedUser) {
        user = authenticatedUser;
    }

    // 2. Google OAuth authentication
    else if (principal instanceof OidcUser oidcUser) {

        String googleSubject = oidcUser.getSubject();

        user = userAuthProviderRepository
                .findByProviderAndProviderUserId(
                        "GOOGLE",
                        googleSubject
                )
                .map(UserAuthProvider::getUser)
                .orElse(null);
    }

    // 3. Email/password authentication
    else {
        String email = authentication.getName();

        if (email != null && !email.isBlank()) {
            user = userRepository.findByEmail(
                    email.trim().toLowerCase()
            ).orElse(null);
        }
    }

    if (user == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("message", "Application user account not found")
        );
    }

    if (!user.isActive()) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of("message", "Your account is inactive")
        );
    }

    Map<String, Object> response = new LinkedHashMap<>();

    response.put("id", user.getId());
    response.put("fullName", user.getFullName());
    response.put("email", user.getEmail());
    response.put("mobileNumber", user.getMobileNumber());
    response.put("role", user.getRole().name());
    response.put("emailVerified", user.isEmailVerified());
    response.put("mobileVerified", user.isMobileVerified());

    return ResponseEntity.ok(response);
}
    // ==========================================
    // LOGOUT
    // ==========================================

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



    @PostMapping("/forgot-password")
public ResponseEntity<?> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request) {

    String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

    userRepository.findByEmail(email).ifPresent(user -> {
        if (user.getPasswordHash() != null
                && user.isEmailVerified()
                && user.isActive()) {
            otpService.sendPasswordResetOtp(user);
        }
    });

    return ResponseEntity.ok(
            Map.of("message",
                    "If the account exists, a password reset OTP has been sent.")
    );
}

@PostMapping("/verify-reset-otp")
public ResponseEntity<?> verifyResetOtp(
        @Valid @RequestBody VerifyResetOtpRequest request) {

    String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

    String resetToken = otpService.verifyResetOtpAndGenerateToken(
            email, request.getOtp()
    );

    return ResponseEntity.ok(
            Map.of(
                    "message", "OTP verified successfully.",
                    "resetToken", resetToken
            )
    );
}

@PostMapping("/reset-password")
public ResponseEntity<?> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request) {

    String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

    otpService.resetPassword(
            email,
            request.getResetToken(),
            request.getNewPassword()
    );

    return ResponseEntity.ok(
            Map.of("message", "Password reset successfully.")
    );
}
}
