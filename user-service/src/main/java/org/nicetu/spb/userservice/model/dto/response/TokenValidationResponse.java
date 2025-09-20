package org.nicetu.spb.userservice.model.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenValidationResponse {
    private String message;

    public TokenValidationResponse() {}

    public TokenValidationResponse(String message) {
        this.message = message;
    }
}