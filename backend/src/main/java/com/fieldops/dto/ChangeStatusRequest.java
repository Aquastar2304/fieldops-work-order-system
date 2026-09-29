package com.fieldops.dto;

import com.fieldops.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(@NotNull WorkOrderStatus status) {}
