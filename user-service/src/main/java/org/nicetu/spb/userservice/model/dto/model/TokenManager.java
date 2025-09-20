package org.nicetu.spb.userservice.model.dto.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
public class TokenManager {
    public String TOKEN;
    public String REFRESH_TOKEN;
    private Map<String, String> tokenStore = new HashMap<>();
    private Map<String, String> refreshTokenStore = new HashMap<>();

    public void storeToken(String email, String token) {
        tokenStore.put(email, token);
        TOKEN = token;
    }

    public void storeRefreshToken(String email, String refreshToken) {
        refreshTokenStore.put(email, refreshToken);
        REFRESH_TOKEN = refreshToken;
    }

    public String getTokenByEmail(String email) {
        return tokenStore.get(email);
    }

    public void removeToken(String email) {
        tokenStore.remove(email);
    }
}