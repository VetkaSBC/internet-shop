package org.nicetu.spb.userservice.service;

import org.nicetu.spb.userservice.model.dto.request.*;
import org.nicetu.spb.userservice.model.dto.response.JwtResponseMessage;
import org.nicetu.spb.userservice.model.entity.User;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface UserService {
    User register(SignUp signUp);
    JwtResponseMessage login(Login signInForm);
    void logout();
    User update(Long userId, SignUp update);
    String changePassword(ChangePasswordRequest request);
    String delete(Long id);
    Optional<User> findById(Long userId);
    Optional<User> findByEmail(String email);
    Page<UserDto> findAllUsers(int page, int size, String sortBy, String sortOrder);
    String resetPassword(String token, ResetPasswordRequest request);
    JwtResponseMessage refreshToken(String refreshToken);
}