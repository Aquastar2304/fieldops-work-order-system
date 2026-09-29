package com.fieldops.service;

import com.fieldops.domain.AppUser;
import com.fieldops.dto.LoginRequest;
import com.fieldops.dto.LoginResponse;
import com.fieldops.repository.AppUserRepository;
import com.fieldops.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        AppUser user = users.findByUsername(request.username())
                .filter(AppUser::isEnabled)
                // Same message whether the user is unknown, disabled or the password is wrong,
                // so the endpoint does not confirm which usernames exist.
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        return new LoginResponse(
                jwtService.issue(user), user.getUsername(), user.getRole(), jwtService.getExpirySeconds());
    }
}
