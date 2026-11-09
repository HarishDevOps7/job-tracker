
package com.harish.job_tracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Service
public class SmsService {
@Value("${msg91.auth-key:}")
private String authKey;

@Value("${msg91.template-id:}")
private String templateId;

@Value("${msg91.base-url:https://control.msg91.com/api/v5/otp}")
private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("authkey", authKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String normalizeMobile(String mobile) {
        String number = mobile.replaceAll("\\D", "");

        if (number.length() == 10) {
            number = "91" + number;
        }

        if (!number.matches("91[6-9]\\d{9}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid Indian mobile number"
            );
        }

        return number;
    }

    public void sendOtp(String mobileNumber) {

        String mobile = normalizeMobile(mobileNumber);

        URI url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("template_id", templateId)
                .queryParam("mobile", mobile)
                .queryParam("otp_length", 6)
                .queryParam("otp_expiry", 5)
                .queryParam("realTimeResponse", 1)
                .build()
                .encode()
                .toUri();

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(Map.of(), createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            JsonNode result = objectMapper.readTree(response.getBody());

            if (!"success".equalsIgnoreCase(
                    result.path("type").asText())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "MSG91 failed to send OTP"
                );
            }

        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "MSG91 SMS request failed"
            );
        } catch (Exception ex) {
            if (ex instanceof ResponseStatusException rse) {
                throw rse;
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to communicate with MSG91"
            );
        }
    }

    public boolean verifyOtp(String mobileNumber, String otp) {

        String mobile = normalizeMobile(mobileNumber);

        URI url = UriComponentsBuilder
                .fromUriString(baseUrl + "/verify")
                .queryParam("mobile", mobile)
                .queryParam("otp", otp)
                .build()
                .encode()
                .toUri();

        HttpEntity<Void> request =
                new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            JsonNode result = objectMapper.readTree(response.getBody());

            if ("success".equalsIgnoreCase(
                    result.path("type").asText())) {
                return true;
            }

            if ("201".equals(result.path("code").asText())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "MSG91 authentication failed"
                );
            }

            return false;

        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "MSG91 verification request failed"
            );
        } catch (Exception ex) {
            if (ex instanceof ResponseStatusException rse) {
                throw rse;
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to verify OTP with MSG91"
            );
        }
    }
}
