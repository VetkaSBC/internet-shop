package controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.nicetu.spb.userservice.controller.UserManagerController;
import org.nicetu.spb.userservice.exception.wrapper.TokenErrorOrAccessTimeOut;
import org.nicetu.spb.userservice.exception.wrapper.UserNotFoundException;
import org.nicetu.spb.userservice.http.HeaderGenerator;
import org.nicetu.spb.userservice.model.dto.request.ChangePasswordRequest;
import org.nicetu.spb.userservice.model.dto.request.SignUp;
import org.nicetu.spb.userservice.model.dto.request.UserDto;
import org.nicetu.spb.userservice.model.dto.response.ResponseMessage;
import org.nicetu.spb.userservice.model.entity.User;
import org.nicetu.spb.userservice.security.jwt.JwtProvider;
import org.nicetu.spb.userservice.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
class UserManagerControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private HeaderGenerator headerGenerator;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserManagerController userManagerController;

    private User testUser;
    private UserDto testUserDto;
    private SignUp testSignUp;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+79123456789")
                .build();

        testUserDto = new UserDto();
        testUserDto.setId(1L);
        testUserDto.setFirstName("John");
        testUserDto.setLastName("Doe");
        testUserDto.setEmail("john.doe@example.com");
        testUserDto.setPhoneNumber("+79123456789");

        testSignUp = new SignUp();
        testSignUp.setFirstName("John");
        testSignUp.setLastName("Doe");
        testSignUp.setEmail("john.doe@example.com");
        testSignUp.setPhoneNumber("+79123456789");
    }

    @Test
    void update_Success() {
        Long userId = 1L;
        when(userService.update(eq(userId), any(SignUp.class))).thenReturn(Mono.just(testUser));

        StepVerifier.create(userManagerController.update(userId, testSignUp))
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.OK &&
                                response.getBody().getMessage().contains("successfully")
                )
                .verifyComplete();

        verify(userService).update(eq(userId), any(SignUp.class));
    }

    @Test
    void update_Failure() {
        Long userId = 1L;
        when(userService.update(eq(userId), any(SignUp.class))).thenReturn(Mono.error(new RuntimeException("Update failed")));

        StepVerifier.create(userManagerController.update(userId, testSignUp))
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.BAD_REQUEST
                )
                .verifyComplete();

        verify(userService).update(eq(userId), any(SignUp.class));
    }

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        when(userService.changePassword(any(ChangePasswordRequest.class))).thenReturn(Mono.just("Password changed successfully"));

        StepVerifier.create(userManagerController.changePassword(request))
                .expectNextMatches(response ->
                        response.equals("Password changed successfully")
                )
                .verifyComplete();

        verify(userService).changePassword(any(ChangePasswordRequest.class));
    }

    @Test
    void delete_Success() {
        Long userId = 1L;
        when(userService.delete(userId)).thenReturn("User deleted successfully");

        String result = userManagerController.delete(userId);

        assertEquals("User deleted successfully", result);
        verify(userService).delete(userId);
    }

    @Test
    void getUserByUsername_Success() {
        String username = "john.doe@example.com";
        when(userService.findByEmail(username)).thenReturn(Optional.of(testUser));
        when(modelMapper.map(testUser, UserDto.class)).thenReturn(testUserDto);

        ResponseEntity<?> response = userManagerController.getUserByUsername(username);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUserDto, response.getBody());
        verify(userService).findByEmail(username);
        verify(modelMapper).map(testUser, UserDto.class);
    }

    @Test
    void getUserByUsername_UserNotFound() {
        String username = "nonexistent@example.com";
        when(userService.findByEmail(username)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                userManagerController.getUserByUsername(username)
        );

        verify(userService).findByEmail(username);
    }

    @Test
    void getUserById_Success() {
        Long userId = 1L;
        when(userService.findById(userId)).thenReturn(Optional.of(testUser));
        when(modelMapper.map(testUser, UserDto.class)).thenReturn(testUserDto);

        ResponseEntity<?> response = userManagerController.getUserById(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUserDto, response.getBody());
        verify(userService).findById(userId);
        verify(modelMapper).map(testUser, UserDto.class);
    }

    @Test
    void getUserById_UserNotFound() {
        Long userId = 1L;
        when(userService.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                userManagerController.getUserById(userId)
        );

        verify(userService).findById(userId);
    }

    @Test
    void getAllUsers_Success() {
        int page = 0;
        int size = 10;
        String sortBy = "id";
        String sortOrder = "ASC";

        Page<UserDto> userPage = new PageImpl<>(List.of(testUserDto));
        when(userService.findAllUsers(page, size, sortBy, sortOrder)).thenReturn(userPage);

        ResponseEntity<Page<UserDto>> response = userManagerController.getAllUsers(page, size, sortBy, sortOrder);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userPage, response.getBody());
        verify(userService).findAllUsers(page, size, sortBy, sortOrder);
    }

    @Test
    void getUserInfo_Success() {
        String token = "Bearer validToken";
        when(jwtProvider.getEmailFromToken(token)).thenReturn("john.doe@example.com");
        when(userService.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(testUser, UserDto.class)).thenReturn(testUserDto);

        ResponseEntity<?> response = userManagerController.getUserInfo(token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testUserDto, response.getBody());
        verify(jwtProvider).getEmailFromToken(token);
        verify(userService).findByEmail("john.doe@example.com");
        verify(modelMapper).map(testUser, UserDto.class);
    }

    @Test
    void getUserInfo_TokenError() {
        String token = "Bearer invalidToken";
        when(jwtProvider.getEmailFromToken(token)).thenReturn("john.doe@example.com");
        when(userService.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());

        assertThrows(TokenErrorOrAccessTimeOut.class, () ->
                userManagerController.getUserInfo(token)
        );

        verify(jwtProvider).getEmailFromToken(token);
        verify(userService).findByEmail("john.doe@example.com");
    }

    @Test
    void getUserInfo_MissingToken() {
        String token = null;

        ResponseEntity<?> response = userManagerController.getUserInfo(token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof ResponseMessage);
        assertEquals("Authorization header is required", ((ResponseMessage) response.getBody()).getMessage());

        verify(jwtProvider, never()).getEmailFromToken(anyString());
        verify(userService, never()).findByEmail(anyString());
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void changePassword_Failure() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        when(userService.changePassword(any(ChangePasswordRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Change password failed")));

        StepVerifier.create(userManagerController.changePassword(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Change password failed")
                )
                .verify();

        verify(userService).changePassword(any(ChangePasswordRequest.class));
    }

    @Test
    void getAllUsers_EmptyPage() {
        int page = 0;
        int size = 10;
        String sortBy = "id";
        String sortOrder = "ASC";

        Page<UserDto> emptyPage = new PageImpl<>(List.of());
        when(userService.findAllUsers(page, size, sortBy, sortOrder)).thenReturn(emptyPage);

        ResponseEntity<Page<UserDto>> response = userManagerController.getAllUsers(page, size, sortBy, sortOrder);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
        verify(userService).findAllUsers(page, size, sortBy, sortOrder);
    }
}