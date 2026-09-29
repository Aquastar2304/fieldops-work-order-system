package com.fieldops.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TechnicianRequest(
        @NotBlank @Size(max = 40) String employeeNo,
        @NotBlank @Size(max = 120) String fullName,
        @Email @Size(max = 120) String email,
        @Size(max = 80) String specialty) {}
