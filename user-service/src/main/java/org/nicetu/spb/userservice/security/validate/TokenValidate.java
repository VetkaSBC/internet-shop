package org.nicetu.spb.userservice.security.validate;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class TokenValidate {

    @Value("${jwt.secret}")
    private String SECRET_KEY;



    public boolean validateToken(String token) {
        if (SECRET_KEY == null || SECRET_KEY.isEmpty()) {
            throw new IllegalArgumentException("Not found secret key un structure");
        }

        if (token.startsWith("Bearer ")) {
            token = token.replace("Bearer ", "");
        }

        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token);


            return !jws.getPayload().getExpiration().before(new Date());

        } catch (ExpiredJwtException ex) {
            throw new IllegalArgumentException("Token has expired");
        } catch (MalformedJwtException ex) {
            throw new IllegalArgumentException("Invalid token structure");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Token validation failed: " + ex.getMessage());
        }

    }
}