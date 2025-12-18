package org.nicetu.spb.userservice.service.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.nicetu.spb.userservice.constant.KafkaConstant;
import org.nicetu.spb.userservice.event.EventProducer;
import org.nicetu.spb.userservice.exception.wrapper.*;
import org.nicetu.spb.userservice.model.dto.request.*;
import org.nicetu.spb.userservice.model.dto.response.InformationMessage;
import org.nicetu.spb.userservice.model.dto.response.JwtResponseMessage;
import org.nicetu.spb.userservice.model.entity.RoleName;
import org.nicetu.spb.userservice.model.entity.User;
import org.nicetu.spb.userservice.repository.UserRepository;
import org.nicetu.spb.userservice.security.jwt.JwtProvider;
import org.nicetu.spb.userservice.security.userprinciple.UserDetailService;
import org.nicetu.spb.userservice.security.userprinciple.UserPrinciple;
import org.nicetu.spb.userservice.service.RoleService;
import org.nicetu.spb.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserDetailService userDetailsService;
    private final ModelMapper modelMapper;
    private final RoleService roleService;
    private final EventProducer eventProducer;

    private final Gson gson = new Gson();

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtProvider jwtProvider,
                           UserDetailService userDetailService,
                           ModelMapper modelMapper,
                           RoleService roleService,
                           EventProducer eventProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailService;
        this.modelMapper = modelMapper;
        this.roleService = roleService;
        this.eventProducer = eventProducer;
    }

    @Override
    @Transactional
    public User register(SignUp signUp) {
        if (userRepository.existsByEmail(signUp.getEmail())) {
            throw new EmailNotFoundException("This email " + signUp.getEmail() + " already exists");
        }
        if (userRepository.existsByPhoneNumber(signUp.getPhoneNumber())) {
            throw new PhoneNumberNotFoundException("This phone number " + signUp.getPhoneNumber() + " already exists");
        }

        User user = modelMapper.map(signUp, User.class);
        user.setPassword(passwordEncoder.encode(signUp.getPassword()));
        user.setRoles(signUp.getRoles()
                .stream()
                .map(role -> roleService.findByName(mapToRoleName(role))
                        .orElseThrow(() -> new RuntimeException("Role not found in the database.")))
                .collect(Collectors.toSet()));

        return userRepository.save(user);
    }

    private RoleName mapToRoleName(String roleName) {
        return switch (roleName) {
            case "ADMIN", "admin", "Admin" -> RoleName.ADMIN;
            case "USER", "user", "User" -> RoleName.USER;
            default -> null;
        };
    }

    @Override
    public JwtResponseMessage login(Login signInForm) {
        String email = signInForm.getEmail();
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!passwordEncoder.matches(signInForm.getPassword(), userDetails.getPassword())) {
            throw new PasswordNotFoundException("Incorrect password");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                signInForm.getPassword(),
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtProvider.createToken(authentication);
        String refreshToken = jwtProvider.creteRefreshToken(authentication);

        UserPrinciple userPrinciple = (UserPrinciple) userDetails;

        return JwtResponseMessage.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .information(InformationMessage.builder()
                        .id(userPrinciple.id())
                        .firstName(userPrinciple.firstname())
                        .lastName(userPrinciple.lastname())
                        .email(userPrinciple.email())
                        .phoneNumber(userPrinciple.phoneNumber())
                        .roles(userPrinciple.roles())
                        .build())
                .build();
    }

    @Override
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(null);
        SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional
    public User update(Long id, SignUp updateDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found userId: " + id + " for update"));

        modelMapper.map(updateDTO, existingUser);
        existingUser.setPassword(passwordEncoder.encode(updateDTO.getPassword()));

        return userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public String changePassword(ChangePasswordRequest request) {
        UserDetails userDetails = getCurrentUserDetails();
        String email = userDetails.getUsername();

        User existingUser = findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email " + email));

        if (!passwordEncoder.matches(request.getOldPassword(), existingUser.getPassword())) {
            throw new PasswordNotFoundException("Incorrect password");
        }

        if (!validateNewPassword(request.getNewPassword(), request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        existingUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(existingUser);

        // Асинхронная отправка email через Kafka
        try {
            EmailDetails emailDetails = emailDetailsConfig(email);
            eventProducer.send(KafkaConstant.PROFILE_ONBOARDING_TOPIC, gson.toJson(emailDetails));
        } catch (Exception e) {
            log.error("Failed to send email notification", e);
        }

        return "Password changed successfully";
    }

    private EmailDetails emailDetailsConfig(String username) {
        return EmailDetails.builder()
                .recipient(username)
                .msgBody(textSendEmailChangePasswordSuccessfully(username))
                .subject("Password Change Successful: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .attachment("Please be careful, don't let this information leak")
                .build();
    }

    private String textSendEmailChangePasswordSuccessfully(String username) {
        return "Hey " + username + "!\n\n" +
                "This is a confirmation that your password has been successfully changed.\n" +
                " If you did not initiate this change, please contact our support team immediately.\n" +
                "If you have any questions or concerns, feel free to reach out to us.\n\n";
    }

    private UserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails) {
            return (UserDetails) authentication.getPrincipal();
        } else {
            throw new UserNotAuthenticatedException("User not authenticated.");
        }
    }

    private boolean validateNewPassword(String newPassword, String confirmPassword) {
        return Objects.equals(newPassword, confirmPassword);
    }

    @Override
    @Transactional
    public String delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        try {
            userRepository.delete(user);
        } catch (DataAccessException e) {
            throw new RuntimeException("Error deleting user with userId: " + id, e);
        }

        return "User with id " + id + " deleted successfully";
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> findByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail);
    }

    @Override
    public Page<UserDto> findAllUsers(int page, int size, String sortBy, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        return userRepository.findAll(pageRequest)
                .map(user -> modelMapper.map(user, UserDto.class));
    }

    @Override
    public String resetPassword(String token, ResetPasswordRequest request) {
        if (!jwtProvider.validateToken(token)) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        String email = jwtProvider.getEmailFromToken(token);
        User user = findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Асинхронная отправка email через Kafka
        try {
            EmailDetails emailDetails = emailDetailsConfig(email);
            eventProducer.send(KafkaConstant.PROFILE_ONBOARDING_TOPIC, gson.toJson(emailDetails));
        } catch (Exception e) {
            log.error("Failed to send email notification", e);
        }

        return "Password has been reset successfully";
    }

    @Override
    public JwtResponseMessage refreshToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String email = jwtProvider.getEmailFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        String newAccessToken = jwtProvider.createToken(authentication);
        String newRefreshToken = jwtProvider.creteRefreshToken(authentication);

        UserPrinciple userPrinciple = (UserPrinciple) userDetails;

        return JwtResponseMessage.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .information(InformationMessage.builder()
                        .id(userPrinciple.id())
                        .firstName(userPrinciple.firstname())
                        .lastName(userPrinciple.lastname())
                        .email(userPrinciple.email())
                        .phoneNumber(userPrinciple.phoneNumber())
                        .roles(userPrinciple.roles())
                        .build())
                .build();
    }
}