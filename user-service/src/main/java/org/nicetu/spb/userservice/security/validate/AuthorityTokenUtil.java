package org.nicetu.spb.userservice.security.validate;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class AuthorityTokenUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public List<String> checkPermission(String token) {
        try {
            System.out.println("Received token: " + token);

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                System.out.println("Extracted token: " + token);
            }

            Claims jws = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            List<String> authorities = jws.get("authorities", List.class);
            System.out.println("Authorities found: " + authorities);
            return authorities;

        } catch (Exception e) {
            System.out.println("Error parsing token: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}