package org.nicetu.spb.userservice.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.userservice.model.dto.request.Login;
import org.nicetu.spb.userservice.model.dto.request.ResetPasswordRequest;
import org.nicetu.spb.userservice.model.dto.request.SignUp;
import org.nicetu.spb.userservice.model.dto.response.InformationMessage;
import org.nicetu.spb.userservice.model.dto.response.JwtResponseMessage;
import org.nicetu.spb.userservice.model.dto.response.ResponseMessage;
import org.nicetu.spb.userservice.model.dto.response.TokenValidationResponse;
import org.nicetu.spb.userservice.security.jwt.JwtProvider;
import org.nicetu.spb.userservice.security.validate.AuthorityTokenUtil;
import org.nicetu.spb.userservice.security.validate.TokenValidate;
import org.nicetu.spb.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/auth")
public class UserAuthController {
    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final TokenValidate tokenValidate;
    private final AuthorityTokenUtil authorityTokenUtil;

    @Autowired
    public UserAuthController(UserService userService,
                              JwtProvider jwtProvider,
                              TokenValidate tokenValidate,
                              AuthorityTokenUtil authorityTokenUtil) {
        this.userService = userService;
        this.jwtProvider = jwtProvider;
        this.tokenValidate = tokenValidate;
        this.authorityTokenUtil = authorityTokenUtil;
    }

    @PostMapping({"/signup", "/register"})
    public ResponseEntity<ResponseMessage> register(@Valid @RequestBody SignUp signUp) {
        try {
            userService.register(signUp);
            return ResponseEntity.ok(new ResponseMessage("Create user: " + signUp.getEmail() + " successfully."));
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseMessage(error.getMessage()));
        }
    }

    @PostMapping({"/signin", "/login"})
    public ResponseEntity<JwtResponseMessage> login(@Valid @RequestBody Login signInForm) {
        try {
            JwtResponseMessage response = userService.login(signInForm);
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            log.error("Ошибка входа: {}", error.getMessage());
            JwtResponseMessage errorResponse = new JwtResponseMessage(
                    null,
                    null,
                    new InformationMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated() and hasAuthority('USER')")
    public ResponseEntity<String> logout() {
        log.info("Logout endpoint called");
        try {
            userService.logout();
            return ResponseEntity.ok("Logged out successfully.");
        } catch (Exception error) {
            log.error("Logout failed", error);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Logout failed.");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResponseMessage> resetPassword(@RequestParam("token") String token,
                                                         @Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        try {
            String message = userService.resetPassword(token, resetPasswordRequest);
            return ResponseEntity.ok(new ResponseMessage(message));
        } catch (Exception error) {
            log.error("Password reset failed: {}", error.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseMessage(error.getMessage()));
        }
    }

    @PostMapping({"/refresh", "/refresh-token"})
    public ResponseEntity<JwtResponseMessage> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        try {
            JwtResponseMessage response = userService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            log.error("Token refresh failed: {}", error.getMessage());
            JwtResponseMessage errorResponse = new JwtResponseMessage(
                    null,
                    null,
                    new InformationMessage()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @GetMapping({"/validateToken", "/validate-token"})
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestHeader(name = "Authorization") String authorizationToken) {
        try {
            if (tokenValidate.validateToken(authorizationToken)) {
                return ResponseEntity.ok(new TokenValidationResponse("Valid token"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new TokenValidationResponse("Invalid token"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenValidationResponse(e.getMessage()));
        }
    }

    @GetMapping({"/hasAuthority", "/authorization"})
    public ResponseEntity<?> getAuthority(@RequestHeader(name = "Authorization") String authorizationToken,
                                          @RequestParam String requiredRole) {
        List<String> authorities = authorityTokenUtil.checkPermission(authorizationToken);

        if (authorities.contains(requiredRole)) {
            return ResponseEntity.ok(new TokenValidationResponse("Role access api"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new TokenValidationResponse("Invalid token"));
        }
    }
}