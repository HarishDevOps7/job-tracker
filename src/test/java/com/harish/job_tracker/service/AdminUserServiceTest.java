
package com.harish.job_tracker.service;

import com.harish.job_tracker.entity.User;
import com.harish.job_tracker.entity.UserRole;
import com.harish.job_tracker.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private User admin;
    private User normalUser;

    @BeforeEach
    void setUp() {
        admin = mock(User.class);
        normalUser = mock(User.class);

        lenient().when(admin.getId()).thenReturn(1L);
        lenient().when(admin.getEmail()).thenReturn("admin@example.com");
        lenient().when(admin.getRole()).thenReturn(UserRole.ADMIN);
        lenient().when(admin.isActive()).thenReturn(true);

        lenient().when(normalUser.getId()).thenReturn(2L);
        lenient().when(normalUser.getEmail()).thenReturn("user@example.com");
        lenient().when(normalUser.getRole()).thenReturn(UserRole.USER);
        lenient().when(normalUser.isActive()).thenReturn(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email, "password"
                )
        );
    }

    private void assertStatus(
            HttpStatus status,
            org.junit.jupiter.api.function.Executable action) {

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, action);

        assertEquals(status, exception.getStatusCode());
    }

    // GET ALL USERS

    @Test
    void getAllUsersSuccessfully() {
        when(userRepository.findAll())
                .thenReturn(List.of(admin, normalUser));

        List<User> result = adminUserService.getAllUsers();

        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    // SEARCH USERS

    @Test
    void searchUsersWithKeyword() {
        when(userRepository
                .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        "harish", "harish"))
                .thenReturn(List.of(normalUser));

        List<User> result = adminUserService.searchUsers("  harish  ");

        assertEquals(1, result.size());

        verify(userRepository)
                .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        "harish", "harish");
    }

    @Test
    void searchUsersWithNullKeywordReturnsAllUsers() {
        when(userRepository.findAll())
                .thenReturn(List.of(admin, normalUser));

        assertEquals(2, adminUserService.searchUsers(null).size());
    }

    @Test
    void searchUsersWithBlankKeywordReturnsAllUsers() {
        when(userRepository.findAll())
                .thenReturn(List.of(admin, normalUser));

        assertEquals(2, adminUserService.searchUsers("   ").size());
    }

    // FILTER USERS

    @Test
    void getUsersByRoleSuccessfully() {
        when(userRepository.findByRole(UserRole.ADMIN))
                .thenReturn(List.of(admin));

        assertEquals(
                1,
                adminUserService.getUsersByRole(UserRole.ADMIN).size()
        );
    }

    @Test
    void getUsersByStatusSuccessfully() {
        when(userRepository.findByActive(true))
                .thenReturn(List.of(admin, normalUser));

        assertEquals(2, adminUserService.getUsersByStatus(true).size());
    }

    // UPDATE USER STATUS

    @Test
    void updateUserStatusSuccessfully() {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(normalUser));

        when(userRepository.save(normalUser))
                .thenReturn(normalUser);

        User result = adminUserService.updateUserStatus(2L, false);

        verify(normalUser).setActive(false);
        assertSame(normalUser, result);
    }

    @Test
    void updateUserStatusShouldRejectMissingUser() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertStatus(
                HttpStatus.NOT_FOUND,
                () -> adminUserService.updateUserStatus(99L, false)
        );
    }

    @Test
    void updateUserStatusShouldPreventSelfDeactivation() {
        authenticateAs("admin@example.com");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        assertStatus(
                HttpStatus.CONFLICT,
                () -> adminUserService.updateUserStatus(1L, false)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserStatusShouldPreventRemovingLastAdmin() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        when(userRepository.findAll())
                .thenReturn(List.of(admin));

        assertStatus(
                HttpStatus.CONFLICT,
                () -> adminUserService.updateUserStatus(1L, false)
        );
    }

    @Test
    void updateUserStatusShouldAllowDeactivationWhenAnotherAdminExists() {
        User anotherAdmin = mock(User.class);

        when(anotherAdmin.getId()).thenReturn(3L);
        when(anotherAdmin.getRole()).thenReturn(UserRole.ADMIN);
        when(anotherAdmin.isActive()).thenReturn(true);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        when(userRepository.findAll())
                .thenReturn(List.of(admin, anotherAdmin));

        when(userRepository.save(admin)).thenReturn(admin);

        assertSame(
                admin,
                adminUserService.updateUserStatus(1L, false)
        );

        verify(admin).setActive(false);
    }

    // UPDATE USER ROLE

    @Test
    void updateUserRoleSuccessfully() {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(normalUser));

        when(userRepository.save(normalUser))
                .thenReturn(normalUser);

        assertSame(
                normalUser,
                adminUserService.updateUserRole(2L, UserRole.ADMIN)
        );

        verify(normalUser).setRole(UserRole.ADMIN);
    }

    @Test
    void updateUserRoleShouldRejectNullRole() {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(normalUser));

        assertStatus(
                HttpStatus.BAD_REQUEST,
                () -> adminUserService.updateUserRole(2L, null)
        );
    }

    @Test
    void updateUserRoleShouldPreventSelfDemotion() {
        authenticateAs("admin@example.com");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        assertStatus(
                HttpStatus.CONFLICT,
                () -> adminUserService.updateUserRole(1L, UserRole.USER)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRoleShouldPreventRemovingLastAdmin() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        when(userRepository.findAll())
                .thenReturn(List.of(admin));

        assertStatus(
                HttpStatus.CONFLICT,
                () -> adminUserService.updateUserRole(1L, UserRole.USER)
        );
    }

    @Test
    void updateUserRoleShouldAllowDemotionWhenAnotherAdminExists() {
        User anotherAdmin = mock(User.class);

        when(anotherAdmin.getId()).thenReturn(3L);
        when(anotherAdmin.getRole()).thenReturn(UserRole.ADMIN);
        when(anotherAdmin.isActive()).thenReturn(true);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        when(userRepository.findAll())
                .thenReturn(List.of(admin, anotherAdmin));

        when(userRepository.save(admin)).thenReturn(admin);

        assertSame(
                admin,
                adminUserService.updateUserRole(1L, UserRole.USER)
        );

        verify(admin).setRole(UserRole.USER);
    }

    // DASHBOARD STATISTICS

    @Test
    void getTotalUsersSuccessfully() {
        when(userRepository.count()).thenReturn(10L);

        assertEquals(10L, adminUserService.getTotalUsers());
    }

    @Test
    void getActiveUsersSuccessfully() {
        when(userRepository.countByActive(true)).thenReturn(8L);

        assertEquals(8L, adminUserService.getActiveUsers());
    }

    @Test
    void getInactiveUsersSuccessfully() {
        when(userRepository.countByActive(false)).thenReturn(2L);

        assertEquals(2L, adminUserService.getInactiveUsers());
    }

    @Test
    void getAdminCountSuccessfully() {
        when(userRepository.countByRole(UserRole.ADMIN))
                .thenReturn(3L);

        assertEquals(3L, adminUserService.getAdminCount());
    }



    // ADDITIONAL BRANCH COVERAGE TESTS

@Test
void updateUserStatusShouldAllowActivationWithoutAdminValidation() {
    when(userRepository.findById(2L))
            .thenReturn(Optional.of(normalUser));
    when(userRepository.save(normalUser)).thenReturn(normalUser);

    assertSame(
            normalUser,
            adminUserService.updateUserStatus(2L, true)
    );

    verify(normalUser).setActive(true);
    verify(userRepository, never()).findAll();
}

@Test
void updateUserStatusShouldSkipLastAdminValidationWhenAlreadyInactive() {
    when(admin.isActive()).thenReturn(false);

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(admin));
    when(userRepository.save(admin)).thenReturn(admin);

    assertSame(
            admin,
            adminUserService.updateUserStatus(1L, false)
    );

    verify(userRepository, never()).findAll();
}

@Test
void updateUserStatusShouldAllowDeactivationWithUnauthenticatedContext() {
    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    "admin@example.com", "password"
            )
    );

    User anotherAdmin = mock(User.class);
    when(anotherAdmin.getId()).thenReturn(3L);
    when(anotherAdmin.getRole()).thenReturn(UserRole.ADMIN);
    when(anotherAdmin.isActive()).thenReturn(true);

    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(userRepository.findAll()).thenReturn(List.of(admin, anotherAdmin));
    when(userRepository.save(admin)).thenReturn(admin);

    assertSame(admin, adminUserService.updateUserStatus(1L, false));
}

@Test
void updateUserStatusShouldAllowDeactivationForAnonymousPrincipal() {
    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    "anonymousUser", "N/A", List.of()
            )
    );

    User anotherAdmin = mock(User.class);
    when(anotherAdmin.getId()).thenReturn(3L);
    when(anotherAdmin.getRole()).thenReturn(UserRole.ADMIN);
    when(anotherAdmin.isActive()).thenReturn(true);

    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(userRepository.findAll()).thenReturn(List.of(admin, anotherAdmin));
    when(userRepository.save(admin)).thenReturn(admin);

    assertSame(admin, adminUserService.updateUserStatus(1L, false));
}

@Test
void updateUserStatusShouldAllowDeactivationWhenAuthenticatedAsDifferentUser() {
    authenticateAs("different@example.com");

    User anotherAdmin = mock(User.class);
    when(anotherAdmin.getId()).thenReturn(3L);
    when(anotherAdmin.getRole()).thenReturn(UserRole.ADMIN);
    when(anotherAdmin.isActive()).thenReturn(true);

    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(userRepository.findAll()).thenReturn(List.of(admin, anotherAdmin));
    when(userRepository.save(admin)).thenReturn(admin);

    assertSame(admin, adminUserService.updateUserStatus(1L, false));
}

@Test
void updateUserStatusShouldPreventSelfDeactivationIgnoringEmailCase() {
    authenticateAs("ADMIN@EXAMPLE.COM");

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(admin));

    assertStatus(
            HttpStatus.CONFLICT,
            () -> adminUserService.updateUserStatus(1L, false)
    );
}

@Test
void updateUserRoleShouldSkipAdminValidationForInactiveAdmin() {
    when(admin.isActive()).thenReturn(false);

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(admin));
    when(userRepository.save(admin)).thenReturn(admin);

    assertSame(
            admin,
            adminUserService.updateUserRole(1L, UserRole.USER)
    );

    verify(userRepository, never()).findAll();
}

@Test
void validateAnotherActiveAdminShouldCoverAllFilterConditions() {
    User nonAdmin = mock(User.class);
    User inactiveAdmin = mock(User.class);
    User anotherActiveAdmin = mock(User.class);

    // Non-admin: fails the first filter
    when(nonAdmin.getRole()).thenReturn(UserRole.USER);

    // Inactive admin: passes role filter, fails active filter
    when(inactiveAdmin.getRole()).thenReturn(UserRole.ADMIN);
    when(inactiveAdmin.isActive()).thenReturn(false);

    // Another active admin: passes both filters
    when(anotherActiveAdmin.getId()).thenReturn(3L);
    when(anotherActiveAdmin.getRole()).thenReturn(UserRole.ADMIN);
    when(anotherActiveAdmin.isActive()).thenReturn(true);

    when(userRepository.findById(1L))
            .thenReturn(Optional.of(admin));

    when(userRepository.findAll())
            .thenReturn(List.of(
                    admin,
                    nonAdmin,
                    inactiveAdmin,
                    anotherActiveAdmin
            ));

    when(userRepository.save(admin)).thenReturn(admin);

    assertSame(
            admin,
            adminUserService.updateUserStatus(1L, false)
    );

    verify(admin).setActive(false);
}
}
