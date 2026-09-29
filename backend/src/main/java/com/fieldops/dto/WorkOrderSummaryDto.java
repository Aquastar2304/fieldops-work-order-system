package com.fieldops.dto;

import com.fieldops.domain.WorkOrder;
import com.fieldops.domain.WorkOrderStatus;

import java.time.Instant;

/** Row shape for the work order list screen. */
public record WorkOrderSummaryDto(
        Long id,
        String reference,
        String title,
        WorkOrderStatus status,
        WorkOrder.Priority priority,
        String assetTag,
        String technicianName,
        Instant openedAt,
        Instant closedAt) {}
