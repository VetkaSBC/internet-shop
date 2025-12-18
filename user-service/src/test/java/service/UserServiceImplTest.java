package service;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.nicetu.spb.userservice.event.EventProducer;
import org.nicetu.spb.userservice.exception.wrapper.*;
import org.nicetu.spb.userservice.model.dto.request.ChangePasswordRequest;
import org.nicetu.spb.userservice.model.dto.request.EmailDetails;
import org.nicetu.spb.userservice.model.dto.request.Login;
import org.nicetu.spb.userservice.model.dto.request.SignUp;
import org.nicetu.spb.userservice.model.dto.request.UserDto;
import org.nicetu.spb.userservice.model.dto.response.InformationMessage;
import org.nicetu.spb.userservice.model.dto.response.JwtResponseMessage;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
    private UserPrinciple userPrinciple;

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

        userPrinciple = UserPrinciple.builder()
                .id(1L)
                .firstname("John")
                .lastname("Doe")
                .email("john.doe@example.com")
                .password("encodedPassword")
                .phoneNumber("+79123456789")
                .roles(List.of(new SimpleGrantedAuthority("USER")))
                .build();
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

        User result = userService.register(testSignUp);

        assertNotNull(result);
        assertEquals(testUser, result);
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(testSignUp.getPassword());
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(testSignUp.getEmail())).thenReturn(true);

        assertThrows(EmailNotFoundException.class, () -> {
            userService.register(testSignUp);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_PhoneNumberAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(testSignUp.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(testSignUp.getPhoneNumber())).thenReturn(true);

        assertThrows(PhoneNumberNotFoundException.class, () -> {
            userService.register(testSignUp);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        when(userDetailsService.loadUserByUsername(testLogin.getEmail())).thenReturn(userPrinciple);
        when(passwordEncoder.matches(testLogin.getPassword(), userPrinciple.getPassword())).thenReturn(true);
        when(jwtProvider.createToken(any(Authentication.class))).thenReturn("accessToken");
        when(jwtProvider.creteRefreshToken(any(Authentication.class))).thenReturn("refreshToken");

        JwtResponseMessage result = userService.login(testLogin);

        assertNotNull(result);
        assertEquals("accessToken", result.getAccessToken());
        assertEquals("refreshToken", result.getRefreshToken());
        assertNotNull(result.getInformation());
        verify(userDetailsService).loadUserByUsername(testLogin.getEmail());
        verify(passwordEncoder).matches(testLogin.getPassword(), userPrinciple.getPassword());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        when(userDetailsService.loadUserByUsername(testLogin.getEmail())).thenReturn(userPrinciple);
        when(passwordEncoder.matches(testLogin.getPassword(), userPrinciple.getPassword())).thenReturn(false);

        assertThrows(PasswordNotFoundException.class, () -> {
            userService.login(testLogin);
        });

        verify(userDetailsService).loadUserByUsername(testLogin.getEmail());
        verify(passwordEncoder).matches(testLogin.getPassword(), userPrinciple.getPassword());
    }

    @Test
    void logout_Success() {
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        userService.logout();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void update_Success() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(passwordEncoder.encode(testSignUp.getPassword())).thenReturn("newEncodedPassword");

        User result = userService.update(userId, testSignUp);

        assertNotNull(result);
        assertEquals(testUser, result);
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(testSignUp.getPassword());
    }

    @Test
    void update_UserNotFound_ThrowsException() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.update(userId, testSignUp);
        });

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_InvalidOldPassword_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrinciple, null, userPrinciple.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findByEmail(userPrinciple.getUsername())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getOldPassword(), testUser.getPassword())).thenReturn(false);

        assertThrows(PasswordNotFoundException.class, () -> {
            userService.changePassword(request);
        });

        verify(userRepository).findByEmail(userPrinciple.getUsername());
        verify(passwordEncoder).matches(request.getOldPassword(), testUser.getPassword());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_PasswordMismatch_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("differentPassword");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrinciple, null, userPrinciple.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findByEmail(userPrinciple.getUsername())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getOldPassword(), testUser.getPassword())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword(request);
        });

        verify(userRepository).findByEmail(userPrinciple.getUsername());
        verify(passwordEncoder).matches(request.getOldPassword(), testUser.getPassword());
        verify(userRepository, never()).save(any());
    }

    @Test
    void findById_Success() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById(userId);

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findById(userId);
    }

    @Test
    void findByEmail_Success() {
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findByEmail(email);
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
        verify(userRepository).findAll(any(PageRequest.class));
        verify(modelMapper).map(testUser, UserDto.class);
    }

    @Test
    void delete_Success() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        String result = userService.delete(userId);

        assertEquals("User with id " + userId + " deleted successfully", result);
        verify(userRepository).findById(userId);
        verify(userRepository).delete(testUser);
    }

    @Test
    void delete_UserNotFound_ThrowsException() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.delete(userId);
        });

        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void refreshToken_Success() {
        String refreshToken = "validRefreshToken";

        when(jwtProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtProvider.getEmailFromToken(refreshToken)).thenReturn("john.doe@example.com");
        when(userDetailsService.loadUserByUsername("john.doe@example.com")).thenReturn(userPrinciple);
        when(jwtProvider.createToken(any(Authentication.class))).thenReturn("newAccessToken");
        when(jwtProvider.creteRefreshToken(any(Authentication.class))).thenReturn("newRefreshToken");

        JwtResponseMessage result = userService.refreshToken(refreshToken);

        assertNotNull(result);
        assertEquals("newAccessToken", result.getAccessToken());
        assertEquals("newRefreshToken", result.getRefreshToken());
        assertNotNull(result.getInformation());
        verify(jwtProvider).validateToken(refreshToken);
        verify(jwtProvider).getEmailFromToken(refreshToken);
        verify(userDetailsService).loadUserByUsername("john.doe@example.com");
    }

    @Test
    void refreshToken_InvalidToken_ThrowsException() {
        String refreshToken = "invalidRefreshToken";
        when(jwtProvider.validateToken(refreshToken)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.refreshToken(refreshToken);
        });

        verify(jwtProvider).validateToken(refreshToken);
        verify(jwtProvider, never()).getEmailFromToken(anyString());
    }


}