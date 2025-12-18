package org.nicetu.spb.userservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.nicetu.spb.userservice.exception.wrapper.TokenErrorOrAccessTimeOut;
import org.nicetu.spb.userservice.exception.wrapper.UserNotFoundException;
import org.nicetu.spb.userservice.http.HeaderGenerator;
import org.nicetu.spb.userservice.model.dto.request.ChangePasswordRequest;
import org.nicetu.spb.userservice.model.dto.request.SignUp;
import org.nicetu.spb.userservice.model.dto.request.UserDto;
import org.nicetu.spb.userservice.model.dto.response.ResponseMessage;
import org.nicetu.spb.userservice.security.jwt.JwtProvider;
import org.nicetu.spb.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/manager")
public class UserManagerController {
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final HeaderGenerator headerGenerator;
    private final JwtProvider jwtProvider;

    @Autowired
    public UserManagerController(UserService userService, HeaderGenerator headerGenerator, JwtProvider jwtProvider,
                                 ModelMapper modelMapper) {
        this.userService = userService;
        this.headerGenerator = headerGenerator;
        this.jwtProvider = jwtProvider;
        this.modelMapper = modelMapper;
    }

    @PutMapping("update/{id}")
    @PreAuthorize("isAuthenticated() and hasAuthority('USER')")
    public ResponseEntity<ResponseMessage> update(@PathVariable("id") Long id, @RequestBody SignUp updateDTO) {
        try {
            userService.update(id, updateDTO);
            return new ResponseEntity<>(
                    new ResponseMessage("Update user: " + updateDTO.getEmail() + " successfully."),
                    HttpStatus.OK);
        } catch (Exception error) {
            return new ResponseEntity<>(
                    new ResponseMessage("Update user: " + updateDTO.getEmail() + " failed " + error.getMessage()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated() and hasAuthority('USER')")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            String result = userService.changePassword(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("delete/{id}")
    @PreAuthorize("isAuthenticated() and (hasAuthority('USER') or hasAuthority('ADMIN'))")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) {
        try {
            String result = userService.delete(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/user")
    @PreAuthorize("(isAuthenticated() and (hasAuthority('USER') and principal.username == #username) or hasAuthority('ADMIN'))")
    public ResponseEntity<?> getUserByUsername(@RequestParam(value = "username") String username) {
        try {
            UserDto user = userService.findByEmail(username)
                    .map(element -> modelMapper.map(element, UserDto.class))
                    .orElseThrow(() -> new UserNotFoundException("User not found with: " + username));
            return new ResponseEntity<>(user,
                    headerGenerator.getHeadersForSuccessGetMethod(),
                    HttpStatus.OK);
        } catch (UserNotFoundException e) {
            return new ResponseEntity<>(null,
                    headerGenerator.getHeadersForError(),
                    HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/user/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('USER') and principal.id == #id")
    public ResponseEntity<?> getUserById(@PathVariable("id") Long id) {
        try {
            UserDto userDTO = userService.findById(id)
                    .map(element -> modelMapper.map(element, UserDto.class))
                    .orElseThrow(() -> new UserNotFoundException("User not found with: " + id));
            return new ResponseEntity<>(userDTO, headerGenerator.getHeadersForSuccessGetMethod(), HttpStatus.OK);
        } catch (UserNotFoundException e) {
            return new ResponseEntity<>(null, headerGenerator.getHeadersForError(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<UserDto>> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(defaultValue = "id") String sortBy,
                                                     @RequestParam(defaultValue = "ASC") String sortOrder) {
        Page<UserDto> usersPage = userService.findAllUsers(page, size, sortBy, sortOrder);
        return new ResponseEntity<>(usersPage, headerGenerator.getHeadersForSuccessGetMethod(), HttpStatus.OK);
    }

    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        log.info("Received token: {}", token);

        if (token == null) {
            log.warn("Authorization header is null");
            return new ResponseEntity<>(
                    new ResponseMessage("Authorization header is required"),
                    HttpStatus.BAD_REQUEST
            );
        }
        try {
            String username = jwtProvider.getEmailFromToken(token);
            UserDto user = userService.findByEmail(username)
                    .map(element -> modelMapper.map(element, UserDto.class))
                    .orElseThrow(() -> new TokenErrorOrAccessTimeOut("Token error or access timeout"));

            return new ResponseEntity<>(user, headerGenerator.getHeadersForSuccessGetMethod(), HttpStatus.OK);
        } catch (TokenErrorOrAccessTimeOut e) {
            return new ResponseEntity<>(null, headerGenerator.getHeadersForError(), HttpStatus.NOT_FOUND);
        }
    }
}