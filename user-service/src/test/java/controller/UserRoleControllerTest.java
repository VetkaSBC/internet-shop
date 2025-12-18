package controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.userservice.controller.UserRoleController;
import org.nicetu.spb.userservice.http.HeaderGenerator;
import org.nicetu.spb.userservice.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleControllerTest {

    @Mock
    private RoleService roleService;

    @Mock
    private HeaderGenerator headerGenerator;

    @InjectMocks
    private UserRoleController userRoleController;


    @Test
    void assignRoles_Success() {
        Long userId = 1L;
        String roleNames = "ADMIN";
        when(roleService.assignRole(userId, roleNames)).thenReturn(true);

        ResponseEntity<?> response = userRoleController.assignRoles(userId, roleNames);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Roles have been assigned to users with IDs" + userId, response.getBody());
        verify(roleService).assignRole(userId, roleNames);
    }

    @Test
    void assignRoles_Failure() {
        Long userId = 1L;
        String roleNames = "ADMIN";
        when(roleService.assignRole(userId, roleNames)).thenReturn(false);

        ResponseEntity<?> response = userRoleController.assignRoles(userId, roleNames);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Has full rights for the User" + userId, response.getBody());
        verify(roleService).assignRole(userId, roleNames);
    }

    @Test
    void revokeRoles_Success() {
        Long userId = 1L;
        String roleNames = "ADMIN";
        when(roleService.revokeRole(userId, roleNames)).thenReturn(true);

        ResponseEntity<?> response = userRoleController.revokeRoles(userId, roleNames);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Roles have been revoke to users with IDs " + userId, response.getBody());
        verify(roleService).revokeRole(userId, roleNames);
    }

    @Test
    void revokeRoles_Failure() {
        Long userId = 1L;
        String roleNames = "ADMIN";
        when(roleService.revokeRole(userId, roleNames)).thenReturn(false);

        ResponseEntity<?> response = userRoleController.revokeRoles(userId, roleNames);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Has full rights for the User" + userId, response.getBody());
        verify(roleService).revokeRole(userId, roleNames);
    }

    @Test
    void getUserRoles_Success() {
        Long userId = 1L;
        List<String> userRoles = List.of("USER", "ADMIN");
        when(roleService.getUserRoles(userId)).thenReturn(userRoles);

        ResponseEntity<List<String>> response = userRoleController.getUserRoles(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userRoles, response.getBody());
        verify(roleService).getUserRoles(userId);
    }

    @Test
    void getUserRoles_EmptyList() {
        Long userId = 1L;
        List<String> userRoles = List.of();
        when(roleService.getUserRoles(userId)).thenReturn(userRoles);

        ResponseEntity<List<String>> response = userRoleController.getUserRoles(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(roleService).getUserRoles(userId);
    }

    @Test
    void assignRoles_MultipleRoleAssignments() {
        Long userId = 1L;
        String roleNames = "USER,ADMIN";
        when(roleService.assignRole(userId, roleNames)).thenReturn(true);

        ResponseEntity<?> response = userRoleController.assignRoles(userId, roleNames);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(roleService).assignRole(userId, roleNames);
    }

    @Test
    void revokeRoles_MultipleRoleRevocations() {
        Long userId = 1L;
        String roleNames = "USER,ADMIN";
        when(roleService.revokeRole(userId, roleNames)).thenReturn(true);

        ResponseEntity<?> response = userRoleController.revokeRoles(userId, roleNames);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(roleService).revokeRole(userId, roleNames);
    }
}