
package com.harish.job_tracker.service;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.VerificationChallenge;
import com.harish.job_tracker.repository.UserRepository;
import com.harish.job_tracker.repository.VerificationChallengeRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import java.util.UUID;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

@Service
public class OtpService {

    private static final Logger log =
            LoggerFactory.getLogger(OtpService.class);

    private final VerificationChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.sms.mode:MOCK}")
    private String smsMode;

    public OtpService(
            VerificationChallengeRepository challengeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            SmsService smsService) {

        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    // ==========================================
    // EMAIL OTP GENERATION
    // ==========================================

    @Transactional
    public void sendEmailOtp(User user) {

        String email = user.getEmail();

        long recentRequests =
                challengeRepository.countByDestinationAndChannelAndCreatedAtAfter(
                        email,
                        "EMAIL",
                        LocalDateTime.now().minusMinutes(15)
                );

        if (recentRequests >= 3) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests. Try again later."
            );
        }

        String otp = generateOtp();

        VerificationChallenge challenge =
                new VerificationChallenge();

        challenge.setUser(user);
        challenge.setDestination(email);
        challenge.setChannel("EMAIL");
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setExpiresAt(
                LocalDateTime.now().plusMinutes(5)
        );
        challenge.setAttempts(0);

        challengeRepository.save(challenge);

        emailService.sendOtp(email, otp);
    }

    // ==========================================
    // EMAIL OTP VERIFICATION
    // ==========================================

    @Transactional
    public boolean verifyEmailOtp(String email, String otp) {

        VerificationChallenge challenge =
                challengeRepository
                        .findTopByDestinationAndChannelAndVerifiedAtIsNullOrderByCreatedAtDesc(
                                email,
                                "EMAIL"
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "OTP not found or already used"
                        ));

        validateOtpExpiry(challenge, "OTP has expired");

        validateMaxAttempts(challenge);

        if (otp == null
                || challenge.getOtpHash() == null
                || !passwordEncoder.matches(
                        otp,
                        challenge.getOtpHash())) {

            recordFailedAttempt(challenge);

            return false;
        }

        User user = challenge.getUser();

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Associated user account not found"
            );
        }

        challenge.setVerifiedAt(LocalDateTime.now());
        challengeRepository.save(challenge);

        user.setEmailVerified(true);
        user.setActive(true);

        userRepository.save(user);

        return true;
    }

    // ==========================================
    // MOBILE NUMBER NORMALIZATION
    // ==========================================

    private String normalizeMobile(String mobileNumber) {

        if (mobileNumber == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mobile number is required"
            );
        }

        String mobile = mobileNumber.replaceAll("\\D", "");

        if (mobile.length() == 12 && mobile.startsWith("91")) {
            mobile = mobile.substring(2);
        }

        if (!mobile.matches("[6-9]\\d{9}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid 10-digit Indian mobile number"
            );
        }

        return mobile;
    }

    // ==========================================
    // MOBILE OTP GENERATION
    // ==========================================

    @Transactional
    public void sendMobileOtp(User user) {

        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User account must be saved first"
            );
        }

        String mobile = normalizeMobile(user.getMobileNumber());

        long recentRequests =
                challengeRepository.countByDestinationAndChannelAndCreatedAtAfter(
                        mobile,
                        "SMS",
                        LocalDateTime.now().minusMinutes(15)
                );

        if (recentRequests >= 3) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests. Try again later."
            );
        }

        if (userRepository.findByMobileNumber(mobile)
                .filter(existing ->
                        !existing.getId().equals(user.getId()))
                .isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mobile number is already registered"
            );
        }

        user.setMobileNumber(mobile);
        userRepository.save(user);

        VerificationChallenge challenge =
                new VerificationChallenge();

        challenge.setUser(user);
        challenge.setDestination(mobile);
        challenge.setChannel("SMS");
        challenge.setExpiresAt(
                LocalDateTime.now().plusMinutes(5)
        );
        challenge.setAttempts(0);

        sendSmsOtp(challenge, mobile, "MOCK SMS OTP");

        challengeRepository.save(challenge);
    }

    // ==========================================
    // MOBILE OTP VERIFICATION
    // ==========================================

    @Transactional
    public boolean verifyMobileOtp(
            String mobileNumber,
            String otp) {

        String mobile = normalizeMobile(mobileNumber);

        VerificationChallenge challenge =
                challengeRepository
                        .findTopByDestinationAndChannelAndVerifiedAtIsNullOrderByCreatedAtDesc(
                                mobile,
                                "SMS"
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Mobile OTP not found or already used"
                        ));

        validateOtpExpiry(challenge, "Mobile OTP has expired");

        validateMaxAttempts(challenge);

        if (otp == null || !otp.matches("\\d{6}")) {

            recordFailedAttempt(challenge);

            return false;
        }

        boolean verified = verifySmsOtp(challenge, mobile, otp);

        if (!verified) {

            recordFailedAttempt(challenge);

            return false;
        }

        User user = challenge.getUser();

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Associated user account not found"
            );
        }

        challenge.setVerifiedAt(LocalDateTime.now());
        challengeRepository.save(challenge);

        user.setMobileVerified(true);
        user.setActive(true);

        userRepository.save(user);

        return true;
    }

@Transactional
public void sendMobileLoginOtp(User user) {

    if (user == null || !user.isActive()
            || !user.isMobileVerified()) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Mobile account is not verified or active"
        );
    }

    String mobile = normalizeMobile(user.getMobileNumber());

    long recentRequests =
            challengeRepository.countByDestinationAndChannelAndCreatedAtAfter(
                    mobile,
                    "SMS_LOGIN",
                    LocalDateTime.now().minusMinutes(15)
            );

    if (recentRequests >= 3) {
        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many login OTP requests. Try again later."
        );
    }

    VerificationChallenge challenge =
            new VerificationChallenge();

    challenge.setUser(user);
    challenge.setDestination(mobile);
    challenge.setChannel("SMS_LOGIN");
    challenge.setExpiresAt(
            LocalDateTime.now().plusMinutes(5)
    );
    challenge.setAttempts(0);

    sendSmsOtp(challenge, mobile, "MOCK LOGIN OTP");

    challengeRepository.save(challenge);
}


@Transactional
public boolean verifyMobileLoginOtp(
        String mobileNumber,
        String otp) {

    String mobile = normalizeMobile(mobileNumber);

    VerificationChallenge challenge =
            challengeRepository
                    .findTopByDestinationAndChannelAndVerifiedAtIsNullOrderByCreatedAtDesc(
                            mobile,
                            "SMS_LOGIN"
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Login OTP not found or already used"
                    ));

    validateOtpExpiry(challenge, "Login OTP has expired");

    validateMaxAttempts(challenge);

    if (otp == null || !otp.matches("\\d{6}")) {
        recordFailedAttempt(challenge);
        return false;
    }

    boolean verified = verifySmsOtp(challenge, mobile, otp);

    if (!verified) {
        recordFailedAttempt(challenge);
        return false;
    }

    challenge.setVerifiedAt(LocalDateTime.now());
    challengeRepository.save(challenge);

    // Do not modify mobileVerified or active during login.
    return true;
}


    // ==========================================
    // PASSWORD RESET OTP GENERATION
    // ==========================================

    @Transactional
    public void sendPasswordResetOtp(User user) {

        String email = user.getEmail();

        long recentRequests =
                challengeRepository.countByDestinationAndChannelAndCreatedAtAfter(
                        email,
                        "PASSWORD_RESET",
                        LocalDateTime.now().minusMinutes(15)
                );

        if (recentRequests >= 3) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests. Try again later."
            );
        }

        String otp = generateOtp();

        VerificationChallenge challenge =
                new VerificationChallenge();

        challenge.setUser(user);
        challenge.setDestination(email);
        challenge.setChannel("PASSWORD_RESET");
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setExpiresAt(
                LocalDateTime.now().plusMinutes(5)
        );
        challenge.setAttempts(0);

        challengeRepository.save(challenge);

        emailService.sendOtp(email, otp);
    }


    // ==========================================
    // VERIFY OTP AND GENERATE RESET TOKEN
    // ==========================================

    @Transactional
    public String verifyResetOtpAndGenerateToken(
            String email,
            String otp) {

        VerificationChallenge challenge =
                challengeRepository
                        .findTopByDestinationAndChannelAndVerifiedAtIsNullOrderByCreatedAtDesc(
                                email,
                                "PASSWORD_RESET"
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "OTP not found or already used"
                        ));

        validateOtpExpiry(challenge, "OTP has expired");

        validateMaxAttempts(challenge);

        if (otp == null
                || !otp.matches("\\d{6}")
                || challenge.getOtpHash() == null
                || !passwordEncoder.matches(
                        otp,
                        challenge.getOtpHash())) {

            recordFailedAttempt(challenge);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid OTP"
            );
        }

        // Mark OTP as verified.
        challenge.setVerifiedAt(LocalDateTime.now());

        // Generate a secure reset token.
        String token = UUID.randomUUID().toString()
                + UUID.randomUUID();

        challenge.setResetTokenHash(hashToken(token));

        // Reset token valid for 10 minutes.
        challenge.setResetTokenExpiresAt(
                LocalDateTime.now().plusMinutes(10)
        );

        challengeRepository.save(challenge);

        return token;
    }


    // ==========================================
    // SHARED OTP HELPERS
    // ==========================================

    private void validateOtpExpiry(
            VerificationChallenge challenge,
            String errorMessage) {

        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    errorMessage
            );
        }
    }

    private void validateMaxAttempts(VerificationChallenge challenge) {
        if (challenge.getAttempts() >= 5) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Maximum OTP attempts exceeded"
            );
        }
    }

    private void recordFailedAttempt(VerificationChallenge challenge) {
        challenge.setAttempts(challenge.getAttempts() + 1);
        challengeRepository.save(challenge);
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private void sendSmsOtp(
            VerificationChallenge challenge,
            String mobile,
            String logLabel) {

        if ("MOCK".equalsIgnoreCase(smsMode)) {
            String otp = generateOtp();
            challenge.setOtpHash(passwordEncoder.encode(otp));

            log.warn("{} for {}: {}",
                    logLabel,
                    mobile.substring(0, 2) + "******" + mobile.substring(8),
                    otp);
        } else if ("MSG91".equalsIgnoreCase(smsMode)) {
            smsService.sendOtp(mobile);
            challenge.setOtpHash(null);
        } else {
            throw new IllegalStateException(
                    "Unsupported SMS mode: " + smsMode
            );
        }
    }

    private boolean verifySmsOtp(
            VerificationChallenge challenge,
            String mobile,
            String otp) {

        if ("MOCK".equalsIgnoreCase(smsMode)) {
            return challenge.getOtpHash() != null
                    && passwordEncoder.matches(otp, challenge.getOtpHash());
        }

        if ("MSG91".equalsIgnoreCase(smsMode)) {
            return smsService.verifyOtp(mobile, otp);
        }

        throw new IllegalStateException(
                "Unsupported SMS mode: " + smsMode
        );
    }


    // ==========================================
    // HASH RESET TOKEN
    // ==========================================

    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Unable to hash reset token",
                    exception
            );
        }
    }


    // ==========================================
    // RESET PASSWORD
    // ==========================================

    @Transactional
    public void resetPassword(
            String email,
            String resetToken,
            String newPassword) {

        if (resetToken == null || resetToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reset token is required"
            );
        }

        VerificationChallenge challenge =
                challengeRepository
                        .findTopByDestinationAndChannelAndVerifiedAtIsNotNullOrderByCreatedAtDesc(
                                email,
                                "PASSWORD_RESET"
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid password reset request"
                        ));

        if (challenge.getResetTokenHash() == null
                || challenge.getResetTokenExpiresAt() == null
                || challenge.getResetTokenExpiresAt()
                        .isBefore(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reset token is invalid or expired"
            );
        }

        String suppliedTokenHash = hashToken(resetToken);

        if (!MessageDigest.isEqual(
                challenge.getResetTokenHash()
                        .getBytes(StandardCharsets.UTF_8),
                suppliedTokenHash.getBytes(StandardCharsets.UTF_8))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid reset token"
            );
        }

        User user = challenge.getUser();

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User account not found"
            );
        }

        // Update password using BCrypt/password encoder.
        user.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);

        // Invalidate reset token after successful password change.
        challenge.setResetTokenHash(null);
        challenge.setResetTokenExpiresAt(null);

        challengeRepository.save(challenge);
    }

}


