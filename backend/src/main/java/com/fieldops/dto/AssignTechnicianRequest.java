package com.fieldops.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTechnicianRequest(@NotNull Long technicianId) {}
