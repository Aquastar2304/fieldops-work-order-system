package com.fieldops.exception;

import java.time.Instant;
import java.util.List;

/** Body returned for every error response. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> violations) {

    public record FieldViolation(String field, String message) {}
}
