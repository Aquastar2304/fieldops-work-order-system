package com.fieldops.dto;

import com.fieldops.domain.WorkOrder;
import com.fieldops.domain.WorkOrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Full work order, including the consumed parts and which moves are legal right now. */
public record WorkOrderDetailDto(
        Long id,
        String reference,
        String title,
        String description,
        WorkOrderStatus status,
        WorkOrder.Priority priority,
        AssetDto asset,
        TechnicianDto technician,
        Instant openedAt,
        Instant closedAt,
        List<WorkOrderPartDto> parts,
        Set<WorkOrderStatus> allowedTransitions,
        Long version) {}
