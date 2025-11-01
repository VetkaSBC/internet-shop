package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.nicetu.spb.userservice.event.EventProducer;

import org.nicetu.spb.userservice.exception.wrapper.EmailNotFoundException;
import org.nicetu.spb.userservice.exception.wrapper.PasswordNotFoundException;
import org.nicetu.spb.userservice.exception.wrapper.PhoneNumberNotFoundException;
import org.nicetu.spb.userservice.exception.wrapper.UserNotFoundException;
import org.nicetu.spb.userservice.model.dto.request.ChangePasswordRequest;
import org.nicetu.spb.userservice.model.dto.request.Login;
import org.nicetu.spb.userservice.model.dto.request.SignUp;
import org.nicetu.spb.userservice.model.dto.request.UserDto;
import org.nicetu.spb.userservice.model.entity.Role;
import org.nicetu.spb.userservice.model.entity.RoleName;
import org.nicetu.spb.userservice.model.entity.User;
import org.nicetu.spb.userservice.repository.UserRepository;
import org.nicetu.spb.userservice.security.jwt.JwtProvider;
import org.nicetu.spb.userservice.security.userprinciple.UserDetailService;
import org.nicetu.spb.userservice.security.userprinciple.UserPrinciple;
import org.nicetu.spb.userservice.service.RoleService;
import org.nicetu.spb.userservice.service.impl.UserServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.test.StepVerifier;


import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserDetailService userDetailsService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private RoleService roleService;

    @Mock
    private EventProducer eventProducer;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private SignUp testSignUp;
    private Login testLogin;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("encodedPassword")
                .phoneNumber("+79123456789")
                .roles(new HashSet<>())
                .build();

        testSignUp = new SignUp();
        testSignUp.setFirstName("John");
        testSignUp.setLastName("Doe");
        testSignUp.setEmail("john.doe@example.com");
        testSignUp.setPassword("password123");
        testSignUp.setPhoneNumber("+79123456789");
        testSignUp.setRoles(Set.of("USER"));

        testLogin = new Login();
        testLogin.setEmail("john.doe@example.com");
        testLogin.setPassword("password123");
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(testSignUp.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(testSignUp.getPhoneNumber())).thenReturn(false);
        when(modelMapper.map(testSignUp, User.class)).thenReturn(testUser);
        when(passwordEncoder.encode(testSignUp.getPassword())).thenReturn("encodedPassword");

        Role userRole = new Role(1L, RoleName.USER);
        when(roleService.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        StepVerifier.create(userService.register(testSignUp))
                .expectNext(testUser)
                .verifyComplete();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(testSignUp.getEmail())).thenReturn(true);

        StepVerifier.create(userService.register(testSignUp))
                .expectError(EmailNotFoundException.class)
                .verify();
    }

    @Test
    void register_PhoneNumberAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(testSignUp.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(testSignUp.getPhoneNumber())).thenReturn(true);

        StepVerifier.create(userService.register(testSignUp))
                .expectError(PhoneNumberNotFoundException.class)
                .verify();
    }

    @Test
    void login_Success() {
        UserPrinciple userPrinciple = UserPrinciple.builder()
                .id(1L)
                .firstname("John")
                .lastname("Doe")
                .email("john.doe@example.com")
                .password("encodedPassword")
                .phoneNumber("+79123456789")
                .roles(List.of(new SimpleGrantedAuthority("USER")))
                .build();

        when(userDetailsService.loadUserByUsername(testLogin.getEmail())).thenReturn(userPrinciple);
        when(passwordEncoder.matches(testLogin.getPassword(), userPrinciple.getPassword())).thenReturn(true);
        when(jwtProvider.createToken(any(Authentication.class))).thenReturn("accessToken");
        when(jwtProvider.creteRefreshToken(any(Authentication.class))).thenReturn("refreshToken");

        StepVerifier.create(userService.login(testLogin))
                .expectNextMatches(response ->
                        response.getAccessToken().equals("accessToken") &&
                                response.getRefreshToken().equals("refreshToken")
                )
                .verifyComplete();
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        UserPrinciple userPrinciple = UserPrinciple.builder()
                .id(1L)
                .firstname("John")
                .lastname("Doe")
                .email("john.doe@example.com")
                .password("encodedPassword")
                .build();

        when(userDetailsService.loadUserByUsername(testLogin.getEmail())).thenReturn(userPrinciple);
        when(passwordEncoder.matches(testLogin.getPassword(), userPrinciple.getPassword())).thenReturn(false);

        StepVerifier.create(userService.login(testLogin))
                .expectError(PasswordNotFoundException.class)
                .verify();
    }

    @Test
    void logout_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        StepVerifier.create(userService.logout())
                .verifyComplete();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void update_Success() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(passwordEncoder.encode(testSignUp.getPassword())).thenReturn("newEncodedPassword");

        StepVerifier.create(userService.update(userId, testSignUp))
                .expectNext(testUser)
                .verifyComplete();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void update_UserNotFound_ThrowsException() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        StepVerifier.create(userService.update(userId, testSignUp))
                .expectError(UserNotFoundException.class)
                .verify();
    }


    @Test
    void changePassword_InvalidOldPassword_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        UserPrinciple userPrinciple = UserPrinciple.builder()
                .id(1L)
                .firstname("John")
                .lastname("Doe")
                .email("john.doe@example.com")
                .password("encodedPassword")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrinciple, null, userPrinciple.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findByEmail(userPrinciple.getUsername())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getOldPassword(), testUser.getPassword())).thenReturn(false);

        StepVerifier.create(userService.changePassword(request))
                .expectError(PasswordNotFoundException.class)
                .verify();
    }

    @Test
    void findById_Success() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById(userId);

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
    }

    @Test
    void findByEmail_Success() {
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
    }

    @Test
    void findAllUsers_Success() {
        int page = 0;
        int size = 10;
        String sortBy = "id";
        String sortOrder = "ASC";

        Page<User> userPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(userPage);

        UserDto userDto = new UserDto();
        userDto.setId(1L);
        userDto.setFirstName("John");
        userDto.setLastName("Doe");
        userDto.setEmail("john.doe@example.com");
        when(modelMapper.map(testUser, UserDto.class)).thenReturn(userDto);

        Page<UserDto> result = userService.findAllUsers(page, size, sortBy, sortOrder);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(userDto, result.getContent().get(0));
    }

    @Test
    void refreshToken_Success() {
        String refreshToken = "validRefreshToken";

        UserPrinciple userPrinciple = UserPrinciple.builder()
                .id(1L)
                .firstname("John")
                .lastname("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+79123456789")
                .roles(List.of(new SimpleGrantedAuthority("USER")))
                .build();

        when(jwtProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtProvider.getEmailFromToken(refreshToken)).thenReturn("john.doe@example.com");
        when(userDetailsService.loadUserByUsername("john.doe@example.com")).thenReturn(userPrinciple);
        when(jwtProvider.createToken(any(Authentication.class))).thenReturn("newAccessToken");
        when(jwtProvider.creteRefreshToken(any(Authentication.class))).thenReturn("newRefreshToken");

        StepVerifier.create(userService.refreshToken(refreshToken))
                .expectNextMatches(response ->
                        response.getAccessToken().equals("newAccessToken") &&
                                response.getRefreshToken().equals("newRefreshToken")
                )
                .verifyComplete();
    }
}