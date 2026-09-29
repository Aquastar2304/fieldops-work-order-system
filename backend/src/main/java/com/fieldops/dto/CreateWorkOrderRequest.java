package com.fieldops.dto;

import com.fieldops.domain.WorkOrder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWorkOrderRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @NotNull Long assetId,
        Long technicianId,
        @NotNull WorkOrder.Priority priority) {}
