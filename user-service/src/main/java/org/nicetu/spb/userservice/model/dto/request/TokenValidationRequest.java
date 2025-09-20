package org.nicetu.spb.userservice.model.dto.request;

public class TokenValidationRequest {
    private String accessToken;

    public TokenValidationRequest() {
    }

    public TokenValidationRequest(String accessToken) {
        this.accessToken = accessToken;
    }
}
