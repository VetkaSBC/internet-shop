package org.nicetu.spb.userservice.service.impl;

import com.google.gson.Gson;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserDetailService userDetailsService;
    private final ModelMapper modelMapper;
    private final RoleService roleService;

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    Gson gson = new Gson();
    @Autowired
    EventProducer eventProducer;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${refresh.token.url}")
    private String refreshTokenUrl;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtProvider jwtProvider,
                           UserDetailService userDetailService,
                           ModelMapper modelMapper,
                           RoleService roleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailService;
        this.modelMapper = modelMapper;
        this.roleService = roleService;
    }

    @Override
    public Mono<User> register(SignUp signUp) {
        return Mono.defer(() -> {
            if (userRepository.existsByEmail(signUp.getEmail())) {
                return Mono.error(new EmailNotFoundException("This email " + signUp.getEmail() + " already exists"));
            }
            if (userRepository.existsByPhoneNumber(signUp.getPhoneNumber())) {
                return Mono.error(new PhoneNumberNotFoundException("This phone number " + signUp.getPhoneNumber() + " already exists"));
            }

            User user = modelMapper.map(signUp, User.class);
            user.setPassword(passwordEncoder.encode(signUp.getPassword()));
            user.setRoles(signUp.getRoles()
                    .stream()
                    .map(role -> roleService.findByName(mapToRoleName(role))
                            .orElseThrow(() -> new RuntimeException("Role not found in the database.")))
                    .collect(Collectors.toSet()));

            userRepository.save(user);
            return Mono.just(user);
        });
    }

    private RoleName mapToRoleName(String roleName) {
        return switch (roleName) {
            case "ADMIN", "admin", "Admin" -> RoleName.ADMIN;
            case "USER", "user", "User" -> RoleName.USER;
            default -> null;
        };
    }

    @Override
    public Mono<JwtResponseMessage> login(Login signInForm) {
        return Mono.fromCallable(() -> {
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
        }).onErrorResume(e -> {
            if (e instanceof UsernameNotFoundException) {
                return Mono.error(new BadCredentialsException("Пользователь не найден"));
            }
            return Mono.error(e);
        });
    }

    @Override
    public Mono<Void> logout() {
        return Mono.defer(() -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            SecurityContextHolder.getContext().setAuthentication(null);

            String currentToken = getCurrentToken();

            if (authentication != null && authentication.isAuthenticated()) {
                String updateToken = jwtProvider.reduceTokenExpiration(currentToken);
            }

            SecurityContextHolder.clearContext();

            return Mono.empty();
        });
    }

    private String getCurrentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object credentials = authentication.getCredentials();

            if (credentials instanceof String) {
                return (String) credentials;
            }
        }

        return null;
    }

    @Transactional
    @Override
    public Mono<User> update(Long id, SignUp updateDTO) {
        try {
            User existingUser = userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("User not found userId: " + id + " for update"));
            modelMapper.map(updateDTO, existingUser);
            existingUser.setPassword(passwordEncoder.encode(updateDTO.getPassword()));

            return Mono.just(userRepository.save(existingUser));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @Transactional
    @Override
    public Mono<String> changePassword(ChangePasswordRequest request) {
        try {
            UserDetails userDetails = getCurrentUserDetails();
            String email = userDetails.getUsername();

            User existringUser = findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found with email " + email));

            if (passwordEncoder.matches(request.getOldPassword(), existringUser.getPassword())) {
                if (validateNewPassword(request.getNewPassword(), request.getConfirmPassword())) {
                    existringUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    userRepository.save(existringUser);

                    EmailDetails emailDetails = emailDetailsConfig(email);

                    return eventProducer.send(KafkaConstant.PROFILE_ONBOARDING_TOPIC, gson.toJson(emailDetails))
                            .thenReturn("Password changed successfully")
                            .publishOn(Schedulers.boundedElastic());
                }

                return Mono.just("Password changed failed.");
            } else {
                return Mono.error(new PasswordNotFoundException("Incorrect password"));
            }
        } catch (Exception e) {
            return Mono.error(new UserNotAuthenticatedException("Transaction silently rolled back"));
        }
    }

    private EmailDetails emailDetailsConfig(String username) {
        return EmailDetails.builder()
                .recipient(username)
                .msgBody(textSendEmailChangePasswordSuccessfully(username))
                .subject("Password Change Successful: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .attachment("Please be careful, don't let this information leak")
                .build();
    }

    public String textSendEmailChangePasswordSuccessfully(String username) {
        return "Hey " + username + "!\n\n" +
                "This is a confirmation that your password has been successfully changed.\n" +
                " If you did not initiate this change, please contact our support team immediately.\n" +
                "If you have any questions or concerns, feel free to reach out to us.\n\n";
    }

    private UserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails) {
            return (UserDetails) authentication.getPrincipal();
        } else {
            throw new UserNotAuthenticatedException("User not authenticated.");
        }
    }

    private boolean validateNewPassword(String newPassword, String confirmPassword) {
        return Objects.equals(newPassword, confirmPassword);
    }

    @Transactional
    @Override
    public String delete(Long id) {
        userRepository.findById(id)
                .ifPresentOrElse(
                        user -> {
                            try {
                                userRepository.delete(user);
                            } catch (DataAccessException e) {
                                throw new RuntimeException("Error deleting user with userId: " + id, e);
                            }
                        },
                        () -> {
                            throw new UserNotFoundException("User not found with id: " + id);
                        }
                );
        return "User with id " + id + " deleted successfully";
    }

    @Override
    public Optional<User> findById(Long userId) {
        return Optional.of(userRepository.findById(userId))
                .orElseThrow(() -> new UserNotFoundException("User not found with userId: " + userId));
    }

    @Override
    public Optional<User> findByEmail(String userEmail) {
        return Optional.ofNullable(userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with userName: " + userEmail)));
    }

    @Override
    public Page<UserDto> findAllUsers(int page, int size, String sortBy, String sortOrder) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<User> usersPage = userRepository.findAll(pageRequest);

        return usersPage.map(user -> modelMapper.map(user, UserDto.class));
    }


    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPhoneNumber(String phone) {
        return userRepository.existsByPhoneNumber(phone);
    }

    @Override
    public Mono<String> resetPassword(String token, ResetPasswordRequest request) {
        return Mono.fromCallable(() -> {
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

            EmailDetails emailDetails = emailDetailsConfig(email);
            eventProducer.send(KafkaConstant.PROFILE_ONBOARDING_TOPIC, gson.toJson(emailDetails))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();

            return "Password has been reset successfully";
        });
    }

    @Override
    public Mono<JwtResponseMessage> refreshToken(String refreshToken) {
        return Mono.fromCallable(() -> {
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
        });
    }

}