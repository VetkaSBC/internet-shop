package org.nicetu.spb.userservice.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    private String email;
    private String newPassword;
    private String confirmPassword;
}
