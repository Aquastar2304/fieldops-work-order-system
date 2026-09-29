package com.fieldops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the fieldops.security.* configuration block. */
@ConfigurationProperties(prefix = "fieldops.security")
public record SecurityProperties(String jwtSecret, long jwtExpirySeconds) {}
