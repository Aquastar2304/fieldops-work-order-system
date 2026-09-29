package com.fieldops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssetRequest(
        @NotBlank @Size(max = 40) String tag,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 80) String manufacturer,
        @Size(max = 80) String model,
        @NotNull Long siteId) {}
