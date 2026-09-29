package com.fieldops.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddPartRequest(@NotNull Long partId, @Positive int quantity) {}
