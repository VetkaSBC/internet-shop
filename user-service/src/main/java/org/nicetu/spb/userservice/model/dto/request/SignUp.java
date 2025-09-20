package org.nicetu.spb.userservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
@RequiredArgsConstructor
public class SignUp {

    @NotBlank(message = "The firstName must not be left blank")
    @Size(min = 6, max = 50, message = "The fullName must be 6 characters or more")
    private String firstName;

    @NotBlank(message = "The lastname must not be left blank")
    @Size(min = 6, max = 50, message = "The username must be 6 characters or more")
    private String lastName;

    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$",
            message = "Password must contain all uppercase and lowercase letters and numbers")
    private String password;

    @Size(max = 50)
    @Pattern(regexp = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", message = "Invalid email format")
    private String email;


    @Size(min = 10, max = 11, message = "Phone number must be between 10 and 11 digits")
    @Pattern(regexp = "^\\+7[0-9]{10,11}$|^0[0-9]{10,11}$", message = "The phone number is not in the correct format")
    private String phoneNumber;

    private Set<String> roles;

}
