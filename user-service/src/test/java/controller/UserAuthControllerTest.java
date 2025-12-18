package controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.userservice.controller.UserAuthController;
import org.nicetu.spb.userservice.model.dto.request.Login;
import org.nicetu.spb.userservice.model.dto.request.ResetPasswordRequest;
import org.nicetu.spb.userservice.model.dto.request.SignUp;
import org.nicetu.spb.userservice.model.dto.response.InformationMessage;
import org.nicetu.spb.userservice.model.dto.response.JwtResponseMessage;
import org.nicetu.spb.userservice.model.dto.response.ResponseMessage;
import org.nicetu.spb.userservice.model.dto.response.TokenValidationResponse;
import org.nicetu.spb.userservice.model.entity.User;
import org.nicetu.spb.userservice.security.validate.AuthorityTokenUtil;
import org.nicetu.spb.userservice.security.validate.TokenValidate;
import org.nicetu.spb.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
class UserAuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private TokenValidate tokenValidate;

    @Mock
    private AuthorityTokenUtil authorityTokenUtil;

    @InjectMocks
    private UserAuthController userAuthController;

    private SignUp testSignUp;
    private Login testLogin;
    private User testUser;
    private JwtResponseMessage testJwtResponse;

    @BeforeEach
    void setUp() {
        testSignUp = new SignUp();
        testSignUp.setFirstName("John");
        testSignUp.setLastName("Doe");
        testSignUp.setEmail("john.doe@example.com");
        testSignUp.setPassword("password123");
        testSignUp.setPhoneNumber("+79123456789");

        testLogin = new Login();
        testLogin.setEmail("john.doe@example.com");
        testLogin.setPassword("password123");

        testUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        testJwtResponse = JwtResponseMessage.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .information(new InformationMessage())
                .build();
    }

    @Test
    void register_Success() {
        when(userService.register(any(SignUp.class))).thenReturn(testUser);

        ResponseEntity<ResponseMessage> response = userAuthController.register(testSignUp);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("successfully"));
        verify(userService).register(any(SignUp.class));
    }

    @Test
    void register_Failure() {
        when(userService.register(any(SignUp.class))).thenThrow(new RuntimeException("Registration failed"));

        ResponseEntity<ResponseMessage> response = userAuthController.register(testSignUp);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userService).register(any(SignUp.class));
    }

    @Test
    void login_Success() {
        when(userService.login(any(Login.class))).thenReturn(testJwtResponse);

        ResponseEntity<JwtResponseMessage> response = userAuthController.login(testLogin);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testJwtResponse, response.getBody());
        verify(userService).login(any(Login.class));
    }

    @Test
    void login_Failure() {
        when(userService.login(any(Login.class))).thenThrow(new RuntimeException("Login failed"));

        ResponseEntity<JwtResponseMessage> response = userAuthController.login(testLogin);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(userService).login(any(Login.class));
    }

    @Test
    void logout_Success() {
        ResponseEntity<String> response = userAuthController.logout();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Logged out successfully.", response.getBody());
    }

    @Test
    void logout_Failure() {
        // В блокирующей версии logout() не бросает исключения, просто очищает контекст
        ResponseEntity<String> response = userAuthController.logout();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Logged out successfully.", response.getBody());
    }

    @Test
    void resetPassword_Success() {
        String token = "resetToken";
        ResetPasswordRequest request = new ResetPasswordRequest();
        when(userService.resetPassword(eq(token), any(ResetPasswordRequest.class)))
                .thenReturn("Password reset successful");

        ResponseEntity<ResponseMessage> response = userAuthController.resetPassword(token, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService).resetPassword(eq(token), any(ResetPasswordRequest.class));
    }

    @Test
    void resetPassword_Failure() {
        String token = "resetToken";
        ResetPasswordRequest request = new ResetPasswordRequest();
        when(userService.resetPassword(eq(token), any(ResetPasswordRequest.class)))
                .thenThrow(new RuntimeException("Reset failed"));

        ResponseEntity<ResponseMessage> response = userAuthController.resetPassword(token, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userService).resetPassword(eq(token), any(ResetPasswordRequest.class));
    }

    @Test
    void refreshToken_Success() {
        String refreshToken = "refreshToken";
        when(userService.refreshToken(refreshToken)).thenReturn(testJwtResponse);

        ResponseEntity<JwtResponseMessage> response = userAuthController.refresh(refreshToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testJwtResponse, response.getBody());
        verify(userService).refreshToken(refreshToken);
    }

    @Test
    void refreshToken_Failure() {
        String refreshToken = "refreshToken";
        when(userService.refreshToken(refreshToken)).thenThrow(new RuntimeException("Refresh failed"));

        ResponseEntity<JwtResponseMessage> response = userAuthController.refresh(refreshToken);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(userService).refreshToken(refreshToken);
    }

    @Test
    void validateToken_ValidToken() {
        String token = "Bearer validToken";
        when(tokenValidate.validateToken(token)).thenReturn(true);

        ResponseEntity<TokenValidationResponse> response = userAuthController.validateToken(token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Valid token", response.getBody().getMessage());
        verify(tokenValidate).validateToken(token);
    }

    @Test
    void validateToken_InvalidToken() {
        String token = "Bearer invalidToken";
        when(tokenValidate.validateToken(token)).thenReturn(false);

        ResponseEntity<TokenValidationResponse> response = userAuthController.validateToken(token);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid token", response.getBody().getMessage());
        verify(tokenValidate).validateToken(token);
    }

    @Test
    void validateToken_ThrowsException() {
        String token = "Bearer invalidToken";
        when(tokenValidate.validateToken(token)).thenThrow(new IllegalArgumentException("Token validation failed"));

        ResponseEntity<TokenValidationResponse> response = userAuthController.validateToken(token);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Token validation failed", response.getBody().getMessage());
        verify(tokenValidate).validateToken(token);
    }

    @Test
    void getAuthority_HasRequiredRole() {
        String token = "Bearer validToken";
        String requiredRole = "USER";
        List<String> authorities = List.of("USER", "ADMIN");

        when(authorityTokenUtil.checkPermission(token)).thenReturn(authorities);

        ResponseEntity<?> response = userAuthController.getAuthority(token, requiredRole);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof TokenValidationResponse);
        assertEquals("Role access api", ((TokenValidationResponse) response.getBody()).getMessage());
        verify(authorityTokenUtil).checkPermission(token);
    }

    @Test
    void getAuthority_MissingRequiredRole() {
        String token = "Bearer validToken";
        String requiredRole = "ADMIN";
        List<String> authorities = List.of("USER");

        when(authorityTokenUtil.checkPermission(token)).thenReturn(authorities);

        ResponseEntity<?> response = userAuthController.getAuthority(token, requiredRole);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody() instanceof TokenValidationResponse);
        assertEquals("Invalid token", ((TokenValidationResponse) response.getBody()).getMessage());
        verify(authorityTokenUtil).checkPermission(token);
    }
}