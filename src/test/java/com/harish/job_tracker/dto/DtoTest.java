
package com.harish.job_tracker.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void resetPasswordRequestGettersAndSetters() {
        ResetPasswordRequest request = new ResetPasswordRequest();

        request.setEmail("test@gmail.com");
        request.setResetToken("reset-token-123");
        request.setNewPassword("Password@123");

        assertEquals("test@gmail.com", request.getEmail());
        assertEquals("reset-token-123", request.getResetToken());
        assertEquals("Password@123", request.getNewPassword());
    }

    @Test
    void verifyEmailOtpRequestGettersAndSetters() {
        VerifyEmailOtpRequest request = new VerifyEmailOtpRequest();

        request.setEmail("test@gmail.com");
        request.setOtp("123456");

        assertEquals("test@gmail.com", request.getEmail());
        assertEquals("123456", request.getOtp());
    }

    @Test
    void verifyMobileOtpRequestGettersAndSetters() {
        VerifyMobileOtpRequest request = new VerifyMobileOtpRequest();

        request.setMobileNumber("9876543210");
        request.setOtp("123456");

        assertEquals("9876543210", request.getMobileNumber());
        assertEquals("123456", request.getOtp());
    }

    @Test
    void loginRequestGettersAndSetters() {
        LoginRequest request = new LoginRequest();

        request.setEmail("test@gmail.com");
        request.setPassword("Password@123");

        assertEquals("test@gmail.com", request.getEmail());
        assertEquals("Password@123", request.getPassword());
    }

    @Test
    void verifyResetOtpRequestGettersAndSetters() {
        VerifyResetOtpRequest request = new VerifyResetOtpRequest();

        request.setEmail("test@gmail.com");
        request.setOtp("654321");

        assertEquals("test@gmail.com", request.getEmail());
        assertEquals("654321", request.getOtp());
    }

    @Test
    void forgotPasswordRequestGettersAndSetters() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();

        request.setEmail("test@gmail.com");

        assertEquals("test@gmail.com", request.getEmail());
    }
}