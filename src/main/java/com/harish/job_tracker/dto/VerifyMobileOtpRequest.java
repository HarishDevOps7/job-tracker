
package com.harish.job_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyMobileOtpRequest {

    @NotBlank
    @Pattern(regexp = "^(\\+91)?[6-9][0-9]{9}$")
    private String mobileNumber;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$")
    private String otp;

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
