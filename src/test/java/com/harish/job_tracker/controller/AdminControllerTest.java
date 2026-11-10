
package com.harish.job_tracker.controller;

import com.harish.job_tracker.dto.AdminUserResponse;
import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.service.AdminUserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminController adminController;

    private User adminUser;
    private User normalUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        adminUser = createUser(
                1L, "Admin User",
                "admin@example.com",
                UserRole.ADMIN, true
        );

        normalUser = createUser(
                2L, "Normal User",
                "user@example.com",
                UserRole.USER, true
        );

        inactiveUser = createUser(
                3L, "Inactive User",
                "inactive@example.com",
                UserRole.USER, false
        );
    }

    // Helper method to create User objects
    private User createUser(
            Long id,
            String name,
            String email,
            UserRole role,
            boolean active
    ) {
        User user = new User();

        ReflectionTestUtils.setField(user, "id", id);

        user.setFullName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(active);

        ReflectionTestUtils.setField(
                user,
                "createdAt",
                LocalDateTime.now()
        );

        return user;
    }

    // 1. Get all users successfully
    @Test
    void getAllUsersSuccessfully() {

        when(adminUserService.getAllUsers())
                .thenReturn(List.of(adminUser, normalUser));

        ResponseEntity<List<AdminUserResponse>> response =
                adminController.getAllUsers();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());

        assertEquals("admin@example.com",
                response.getBody().get(0).email());

        assertEquals("user@example.com",
                response.getBody().get(1).email());

        verify(adminUserService).getAllUsers();
    }

    // 2. Get all users when list is empty
    @Test
    void getAllUsersWhenNoUsersExist() {

        when(adminUserService.getAllUsers())
                .thenReturn(List.of());

        ResponseEntity<List<AdminUserResponse>> response =
                adminController.getAllUsers();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(adminUserService).getAllUsers();
    }

    // 3. Search users successfully
    @Test
    void searchUsersSuccessfully() {

        when(adminUserService.searchUsers("Admin"))
                .thenReturn(List.of(adminUser));

        ResponseEntity<List<AdminUserResponse>> response =
                adminController.searchUsers("Admin");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        AdminUserResponse result = response.getBody().get(0);

        assertEquals("Admin User", result.fullName());
        assertEquals("admin@example.com", result.email());
        assertEquals("ADMIN", result.role());
        assertTrue(result.active());

        verify(adminUserService).searchUsers("Admin");
    }

    // 4. Search users with no results
    @Test
    void searchUsersWithNoResults() {

        when(adminUserService.searchUsers("unknown"))
                .thenReturn(List.of());

        ResponseEntity<List<AdminUserResponse>> response =
                adminController.searchUsers("unknown");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(adminUserService).searchUsers("unknown");
    }

    // 5. Get users by role
    @Test
    void getUsersByRoleSuccessfully() {

        when(adminUserService.getUsersByRole(UserRole.ADMIN))
                .thenReturn(List.of(adminUser));

        ResponseEntity<List<AdminUserResponse>> response =
                adminController.getUsersByRole(UserRole.ADMIN);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        assertEquals("ADMIN",
                response.getBody().get(0).role());

        verify(adminUserService)
                .getUsersByRole(UserRole.ADMIN);
    }

    // 6. Get active users
    @Test
    void getActiveUsersSuccessfully() {

        when(adminUserService.getUsersByStatus(true))
                .thenReturn(List.of(adminUser, normalUser));

        ResponseEntity<List<AdminUserResponse>> response =
                adminController.getUsersByStatus(true);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());

        assertTrue(response.getBody().stream()
                .allMatch(AdminUserResponse::active));

        verify(adminUserService)
                .getUsersByStatus(true);
    }

    // 7. Get inactive users
    @Test
    void getInactiveUsersSuccessfully() {

        when(adminUserService.getUsersByStatus(false))
                .thenReturn(List.of(inactiveUser));

        ResponseEntity<List<AdminUserResponse>> response =
                adminController.getUsersByStatus(false);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        assertFalse(response.getBody().get(0).active());

        verify(adminUserService)
                .getUsersByStatus(false);
    }

    // 8. Update user status
    @Test
    void updateUserStatusSuccessfully() {

        inactiveUser.setActive(true);

        when(adminUserService.updateUserStatus(3L, true))
                .thenReturn(inactiveUser);

        ResponseEntity<AdminUserResponse> response =
                adminController.updateUserStatus(3L, true);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals("inactive@example.com",
                response.getBody().email());

        assertTrue(response.getBody().active());

        verify(adminUserService)
                .updateUserStatus(3L, true);
    }

    // 9. Update user role
    @Test
    void updateUserRoleSuccessfully() {

        normalUser.setRole(UserRole.ADMIN);

        when(adminUserService.updateUserRole(2L, UserRole.ADMIN))
                .thenReturn(normalUser);

        ResponseEntity<AdminUserResponse> response =
                adminController.updateUserRole(2L, UserRole.ADMIN);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals("ADMIN", response.getBody().role());

        verify(adminUserService)
                .updateUserRole(2L, UserRole.ADMIN);
    }

    // 10. Get user statistics
    @Test
    void getUserStatsSuccessfully() {

        when(adminUserService.getAllUsers())
                .thenReturn(List.of(
                        adminUser,
                        normalUser,
                        inactiveUser
                ));

        ResponseEntity<Map<String, Long>> response =
                adminController.getUserStats();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Long> stats = response.getBody();

        assertEquals(3L, stats.get("totalUsers"));
        assertEquals(2L, stats.get("activeUsers"));
        assertEquals(1L, stats.get("inactiveUsers"));
        assertEquals(1L, stats.get("adminCount"));

        verify(adminUserService).getAllUsers();
    }

    // 11. Statistics when no users exist
    @Test
    void getUserStatsWhenNoUsersExist() {

        when(adminUserService.getAllUsers())
                .thenReturn(List.of());

        ResponseEntity<Map<String, Long>> response =
                adminController.getUserStats();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        Map<String, Long> stats = response.getBody();

        assertEquals(0L, stats.get("totalUsers"));
        assertEquals(0L, stats.get("activeUsers"));
        assertEquals(0L, stats.get("inactiveUsers"));
        assertEquals(0L, stats.get("adminCount"));
    }

    // 12. Statistics when all users are inactive
    @Test
    void getUserStatsWhenAllUsersAreInactive() {

        when(adminUserService.getAllUsers())
                .thenReturn(List.of(inactiveUser));

        ResponseEntity<Map<String, Long>> response =
                adminController.getUserStats();

        assertEquals(200, response.getStatusCode().value());

        Map<String, Long> stats = response.getBody();

        assertEquals(1L, stats.get("totalUsers"));
        assertEquals(0L, stats.get("activeUsers"));
        assertEquals(1L, stats.get("inactiveUsers"));
        assertEquals(0L, stats.get("adminCount"));
    }
}
