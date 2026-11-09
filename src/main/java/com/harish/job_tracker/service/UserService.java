
package com.harish.job_tracker.service;

import com.harish.job_tracker.dto.RegisterRequest;
import com.harish.job_tracker.dto.MobileRegisterRequest;
import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OtpService otpService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    // Email registration
    @Transactional
    public User register(RegisterRequest request) {

        String normalizedEmail = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        User user = new User();

        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(UserRole.USER);
        user.setActive(false);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        otpService.sendEmailOtp(savedUser);

        return savedUser;
    }

    // Mobile registration
    @Transactional
    public User registerMobile(MobileRegisterRequest request) {

        String mobile = request.getMobileNumber()
                .replaceAll("\\D", "");

        if (mobile.startsWith("91") && mobile.length() == 12) {
            mobile = mobile.substring(2);
        }

        if (userRepository.existsByMobileNumber(mobile)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mobile number is already registered"
            );
        }

        User user = new User();

        user.setFullName(request.getFullName().trim());
        user.setMobileNumber(mobile);

        user.setEmail(null);
        user.setPasswordHash(null);

        user.setRole(UserRole.USER);
        user.setActive(false);
        user.setEmailVerified(false);
        user.setMobileVerified(false);

        User savedUser = userRepository.save(user);

        otpService.sendMobileOtp(savedUser);

        return savedUser;
    }
}
