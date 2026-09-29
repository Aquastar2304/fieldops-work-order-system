package com.fieldops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PartRequest(
        @NotBlank @Size(max = 40) String sku,
        @NotBlank @Size(max = 120) String name,
        @PositiveOrZero int quantityInStock) {}
