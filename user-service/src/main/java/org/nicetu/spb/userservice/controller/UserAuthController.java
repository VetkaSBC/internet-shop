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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

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
    public Mono<ResponseEntity<ResponseMessage>> register(@Valid @RequestBody SignUp signUp) {
        return userService.register(signUp)
                .map(user -> ResponseEntity.ok(new ResponseMessage("Create user: " + signUp.getEmail() + " successfully.")))
                .onErrorResume(error -> Mono.just(
                        ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(new ResponseMessage(error.getMessage()))
                ));
    }

    @PostMapping({"/signin", "/login"})
    public Mono<ResponseEntity<JwtResponseMessage>> login(@Valid @RequestBody Login signInForm) {
        return userService.login(signInForm)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Ошибка входа: {}", error.getMessage());
                    JwtResponseMessage errorjwtResponseMessage = new JwtResponseMessage(
                            null,
                            null,
                            new InformationMessage()
                    );
                    return Mono.just(new ResponseEntity<>(errorjwtResponseMessage, HttpStatus.INTERNAL_SERVER_ERROR));
                });
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated() and hasAuthority('USER')")
    public Mono<ResponseEntity<String>> logout() {
        log.info("Logout endpoint called");
        return userService.logout()
                .then(Mono.just(new ResponseEntity<>("Logged out successfully.", HttpStatus.OK)))
                .onErrorResume(error -> {
                    log.error("Logout failed", error);
                    return Mono.just(new ResponseEntity<>("Logout failed.", HttpStatus.BAD_REQUEST));
                });
    }

    @PostMapping("/reset-password")
    public Mono<ResponseEntity<ResponseMessage>> resetPassword(@RequestParam("token") String token,
                                                               @Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        return userService.resetPassword(token, resetPasswordRequest)
                .map(message -> ResponseEntity.ok(new ResponseMessage(message)))
                .onErrorResume(error -> {
                    log.error("Password reset failed: {}", error.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ResponseMessage(error.getMessage())));
                });
    }

    @PostMapping({"/refresh", "/refresh-token"})
    public Mono<ResponseEntity<JwtResponseMessage>> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        return userService.refreshToken(refreshToken)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Token refresh failed: {}", error.getMessage());
                    JwtResponseMessage errorResponse = new JwtResponseMessage(
                            null,
                            null,
                            new InformationMessage()
                    );
                    return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED));
                });
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
                                          String requiredRole) {
        List<String> authorities = authorityTokenUtil.checkPermission(authorizationToken);

        if (authorities.contains(requiredRole)) {
            return ResponseEntity.ok(new TokenValidationResponse("Role access api"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new TokenValidationResponse("Invalid token"));
        }
    }
}