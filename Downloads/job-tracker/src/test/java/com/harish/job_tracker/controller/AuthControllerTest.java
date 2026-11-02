
package com.harish.job_tracker.controller;

import com.harish.job_tracker.dto.RegisterRequest;
import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest request;
    private User user;

   @BeforeEach
void setUp() throws Exception {
    request = new RegisterRequest();
    request.setFullName("Harish");
    request.setEmail("harish@example.com");
    request.setPassword("Harish@123");

    user = new User();

    Field idField = User.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(user, 1L);

    user.setFullName("Harish");
    user.setEmail("harish@example.com");
    user.setPasswordHash("encryptedPassword");
    user.setRole(UserRole.USER);
}

    @Test
    void registerShouldReturnCreatedResponse() {

        when(userService.register(request)).thenReturn(user);

        ResponseEntity<Map<String, Object>> response =
                authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        assertEquals("Registration successful",
                response.getBody().get("message"));

        verify(userService, times(1)).register(request);
    }

    @Test
    void registerShouldReturnUserDetails() {

        when(userService.register(request)).thenReturn(user);

        ResponseEntity<Map<String, Object>> response =
                authController.register(request);

        Map<String, Object> body = response.getBody();

        assertNotNull(body);
        assertEquals(1L, body.get("id"));
        assertEquals("Harish", body.get("fullName"));
        assertEquals("harish@example.com", body.get("email"));
        assertEquals(UserRole.USER, body.get("role"));
    }

    @Test
    void registerShouldNotExposePasswordHash() {

        when(userService.register(request)).thenReturn(user);

        ResponseEntity<Map<String, Object>> response =
                authController.register(request);

        assertNotNull(response.getBody());

        assertFalse(response.getBody().containsKey("passwordHash"));
        assertFalse(response.getBody().containsKey("password"));
    }

    @Test
    void registerShouldPropagateServiceException() {

        when(userService.register(request))
                .thenThrow(new RuntimeException("Registration failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authController.register(request)
        );

        assertEquals("Registration failed", exception.getMessage());

        verify(userService, times(1)).register(request);
    }
}
