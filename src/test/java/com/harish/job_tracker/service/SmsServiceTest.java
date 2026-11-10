
package com.harish.job_tracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class SmsServiceTest {

    private SmsService smsService;

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        smsService = new SmsService();
        restTemplate = mock(RestTemplate.class);

        ReflectionTestUtils.setField(smsService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(smsService, "authKey", "test-auth-key");
        ReflectionTestUtils.setField(smsService, "templateId", "test-template");
        ReflectionTestUtils.setField(
                smsService,
                "baseUrl",
                "https://control.msg91.com/api/v5/otp"
        );
    }

    // SEND OTP TESTS

    @Test
    void sendOtpSuccessfully() {
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(
                "{\"type\":\"success\"}"
        ));

        assertDoesNotThrow(() -> smsService.sendOtp("9876543210"));

        verify(restTemplate).exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(),
                eq(String.class)
        );
    }

    @Test
    void sendOtpShouldAcceptIndianCountryCode() {
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(
                "{\"type\":\"success\"}"
        ));

        assertDoesNotThrow(() -> smsService.sendOtp("+91 9876543210"));
    }

    @Test
    void sendOtpShouldRejectInvalidMobile() {
        assertThrows(
                ResponseStatusException.class,
                () -> smsService.sendOtp("12345")
        );

        verifyNoInteractions(restTemplate);
    }

    @Test
    void sendOtpShouldRejectFailedMsg91Response() {
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(
                "{\"type\":\"error\"}"
        ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> smsService.sendOtp("9876543210")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }

    @Test
    void sendOtpShouldHandleHttpError() {
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(),
                eq(String.class)
        )).thenThrow(new HttpClientErrorException(
                HttpStatus.BAD_REQUEST
        ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> smsService.sendOtp("9876543210")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }

    @Test
    void sendOtpShouldHandleInvalidJsonResponse() {
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.POST),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("invalid-json"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> smsService.sendOtp("9876543210")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }

    // VERIFY OTP TESTS

    @Test
    void verifyOtpSuccessfully() {
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(
                "{\"type\":\"success\"}"
        ));

        assertTrue(smsService.verifyOtp("9876543210", "123456"));
    }

    @Test
    void verifyOtpShouldReturnFalseForInvalidOtp() {
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(
                "{\"type\":\"error\",\"code\":\"400\"}"
        ));

        assertFalse(smsService.verifyOtp("9876543210", "111111"));
    }

    @Test
    void verifyOtpShouldRejectMsg91AuthenticationFailure() {
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(
                "{\"type\":\"error\",\"code\":\"201\"}"
        ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> smsService.verifyOtp("9876543210", "123456")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }

    @Test
    void verifyOtpShouldRejectInvalidMobile() {
        assertThrows(
                ResponseStatusException.class,
                () -> smsService.verifyOtp("12345", "123456")
        );

        verifyNoInteractions(restTemplate);
    }

    @Test
    void verifyOtpShouldHandleHttpError() {
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(),
                eq(String.class)
        )).thenThrow(new HttpClientErrorException(
                HttpStatus.BAD_REQUEST
        ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> smsService.verifyOtp("9876543210", "123456")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }

    @Test
    void verifyOtpShouldHandleInvalidJsonResponse() {
        when(restTemplate.exchange(
                any(URI.class),
                eq(HttpMethod.GET),
                any(),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("invalid-json"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> smsService.verifyOtp("9876543210", "123456")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }
}
