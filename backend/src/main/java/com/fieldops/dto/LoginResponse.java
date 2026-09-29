package com.fieldops.dto;

import com.fieldops.domain.Role;

public record LoginResponse(String token, String username, Role role, long expiresInSeconds) {}
