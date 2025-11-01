package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.userservice.exception.wrapper.RoleNotFoundException;
import org.nicetu.spb.userservice.exception.wrapper.UserNotFoundException;
import org.nicetu.spb.userservice.model.entity.Role;
import org.nicetu.spb.userservice.model.entity.RoleName;
import org.nicetu.spb.userservice.model.entity.User;
import org.nicetu.spb.userservice.repository.RoleRepository;
import org.nicetu.spb.userservice.repository.UserRepository;
import org.nicetu.spb.userservice.service.impl.RoleServiceImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = new Role(1L, RoleName.USER);
        adminRole = new Role(2L, RoleName.ADMIN);

        testUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("encodedPassword")
                .phoneNumber("+79123456789")
                .roles(new HashSet<>())
                .build();
    }

    @Test
    void findByName_Success() {
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));

        Optional<Role> result = roleService.findByName(RoleName.USER);

        assertTrue(result.isPresent());
        assertEquals(userRole, result.get());
        assertEquals(RoleName.USER, result.get().getName());
    }

    @Test
    void findByName_RoleNotFound_ThrowsException() {
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.empty());

        RoleNotFoundException exception = assertThrows(RoleNotFoundException.class, () -> {
            roleService.findByName(RoleName.USER);
        });

        assertEquals("Role Not Found with name: USER", exception.getMessage());
    }

    @Test
    void assignRole_Success() {
        Long userId = 1L;
        String roleName = "USER";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        boolean result = roleService.assignRole(userId, roleName);

        assertTrue(result);
        assertTrue(testUser.getRoles().contains(userRole));
        verify(userRepository).save(testUser);
    }

    @Test
    void assignRole_UserAlreadyHasRole_ReturnsFalse() {
        Long userId = 1L;
        String roleName = "USER";
        testUser.getRoles().add(userRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));

        boolean result = roleService.assignRole(userId, roleName);

        assertFalse(result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void assignRole_UserNotFound_ThrowsException() {
        Long userId = 1L;
        String roleName = "USER";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            roleService.assignRole(userId, roleName);
        });

        assertEquals("User not found with id: " + userId, exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void assignRole_RoleNotFound_ThrowsException() {
        Long userId = 1L;
        String roleName = "USER";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.empty());

        RoleNotFoundException exception = assertThrows(RoleNotFoundException.class, () -> {
            roleService.assignRole(userId, roleName);
        });

        assertEquals("Role not found in system: " + roleName, exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void assignRole_AdminRole_Success() {
        Long userId = 1L;
        String roleName = "ADMIN";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        boolean result = roleService.assignRole(userId, roleName);

        assertTrue(result);
        assertTrue(testUser.getRoles().contains(adminRole));
        verify(userRepository).save(testUser);
    }

    @Test
    void assignRole_CaseInsensitiveRoleName_Success() {
        Long userId = 1L;
        String roleName = "user";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        boolean result = roleService.assignRole(userId, roleName);

        assertTrue(result);
        assertTrue(testUser.getRoles().contains(userRole));
        verify(userRepository).save(testUser);
    }

    @Test
    void revokeRole_Success() {
        Long userId = 1L;
        String roleName = "USER";
        testUser.getRoles().add(userRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        boolean result = roleService.revokeRole(userId, roleName);

        assertTrue(result);
        assertFalse(testUser.getRoles().contains(userRole));
        verify(userRepository).save(testUser);
    }

    @Test
    void revokeRole_UserDoesNotHaveRole_ReturnsFalse() {
        Long userId = 1L;
        String roleName = "USER";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        boolean result = roleService.revokeRole(userId, roleName);

        assertFalse(result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void revokeRole_UserNotFound_ThrowsException() {
        Long userId = 1L;
        String roleName = "USER";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            roleService.revokeRole(userId, roleName);
        });

        assertEquals("User not found.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void revokeRole_MultipleRoles_RemovesOnlySpecifiedRole() {
        Long userId = 1L;
        String roleName = "USER";
        testUser.getRoles().add(userRole);
        testUser.getRoles().add(adminRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        boolean result = roleService.revokeRole(userId, roleName);

        assertTrue(result);
        assertFalse(testUser.getRoles().contains(userRole));
        assertTrue(testUser.getRoles().contains(adminRole));
        verify(userRepository).save(testUser);
    }

    @Test
    void getUserRoles_Success() {
        Long userId = 1L;
        testUser.getRoles().add(userRole);
        testUser.getRoles().add(adminRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        List<String> result = roleService.getUserRoles(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("USER"));
        assertTrue(result.contains("ADMIN"));
    }

    @Test
    void getUserRoles_UserHasNoRoles_ReturnsEmptyList() {
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        List<String> result = roleService.getUserRoles(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserRoles_UserNotFound_ThrowsException() {
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            roleService.getUserRoles(userId);
        });

        assertEquals("User not found.", exception.getMessage());
    }
}