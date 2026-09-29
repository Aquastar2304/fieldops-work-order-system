package com.fieldops.security;

import com.fieldops.config.SecurityProperties;
import com.fieldops.domain.AppUser;
import com.fieldops.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET =
            Base64.getEncoder().encodeToString("a-test-only-signing-key-of-32-bytes!!".getBytes());

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new SecurityProperties(SECRET, 3600));
    }

    private static AppUser user(Role role) {
        return AppUser.builder().id(1L).username("supervisor").passwordHash("x").role(role).build();
    }

    @Test
    void issuesATokenCarryingTheUsernameAndRole() {
        String token = jwtService.issue(user(Role.SUPERVISOR));

        var claims = jwtService.parse(token).orElseThrow();
        assertThat(claims.getSubject()).isEqualTo("supervisor");
        assertThat(claims.get("role", String.class)).isEqualTo("SUPERVISOR");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void rejectsATokenSignedWithADifferentKey() {
        String foreign = new JwtService(new SecurityProperties(
                        Base64.getEncoder().encodeToString("a-different-key-also-32-bytes-long!!".getBytes()), 3600))
                .issue(user(Role.ADMIN));

        assertThat(jwtService.parse(foreign)).isEmpty();
    }

    @Test
    void rejectsATamperedToken() {
        String token = jwtService.issue(user(Role.TECHNICIAN));
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThat(jwtService.parse(tampered)).isEmpty();
    }

    @Test
    void rejectsGarbageRatherThanThrowing() {
        assertThat(jwtService.parse("not-a-token")).isEmpty();
        assertThat(jwtService.parse("")).isEmpty();
    }

    @Test
    void rejectsAnAlreadyExpiredToken() {
        JwtService instant = new JwtService(new SecurityProperties(SECRET, -1));

        String token = instant.issue(user(Role.ADMIN));

        assertThat(instant.parse(token)).isEmpty();
    }
}
