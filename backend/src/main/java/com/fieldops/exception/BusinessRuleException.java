package com.fieldops.exception;

/** Thrown when an operation is well formed but violates a domain rule. Mapped to HTTP 409. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
