package com.fieldops.security;

import com.fieldops.config.SecurityProperties;
import com.fieldops.domain.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/** Issues and validates the bearer tokens used by the API. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirySeconds;

    public JwtService(SecurityProperties properties) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(properties.jwtSecret()));
        this.expirySeconds = properties.jwtExpirySeconds();
    }

    public String issue(AppUser user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirySeconds * 1000))
                .signWith(key)
                .compact();
    }

    /** Returns the token claims, or empty when the token is absent, expired or tampered with. */
    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(
                    Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }
}
