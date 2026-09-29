package com.fieldops.controller;

import com.fieldops.domain.WorkOrderStatus;
import com.fieldops.dto.AddPartRequest;
import com.fieldops.dto.AssignTechnicianRequest;
import com.fieldops.dto.ChangeStatusRequest;
import com.fieldops.dto.CreateWorkOrderRequest;
import com.fieldops.dto.WorkOrderDetailDto;
import com.fieldops.dto.WorkOrderSummaryDto;
import com.fieldops.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
@Tag(name = "Work orders")
public class WorkOrderController {

    private final WorkOrderService service;

    public WorkOrderController(WorkOrderService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List work orders, optionally filtered by status")
    public Page<WorkOrderSummaryDto> list(
            @RequestParam(required = false) WorkOrderStatus status,
            @PageableDefault(size = 20, sort = "openedAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return service.list(status, pageable);
    }

    @GetMapping("/overdue")
    @Operation(summary = "List work orders still open after the given number of days")
    public List<WorkOrderSummaryDto> overdue(@RequestParam(defaultValue = "7") int days) {
        return service.findOverdue(days);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a work order with its parts and legal next statuses")
    public WorkOrderDetailDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Raise a new work order")
    public ResponseEntity<WorkOrderDetailDto> create(@Valid @RequestBody CreateWorkOrderRequest request) {
        WorkOrderDetailDto created = service.create(request);
        return ResponseEntity.created(URI.create("/api/work-orders/" + created.id())).body(created);
    }

    @PostMapping("/{id}/technician")
    @Operation(summary = "Assign a technician")
    public WorkOrderDetailDto assign(
            @PathVariable Long id, @Valid @RequestBody AssignTechnicianRequest request) {
        return service.assignTechnician(id, request);
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Move the work order to another status")
    public WorkOrderDetailDto changeStatus(
            @PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
        return service.changeStatus(id, request);
    }

    @PostMapping("/{id}/parts")
    @Operation(summary = "Consume a part, drawing it out of inventory")
    public WorkOrderDetailDto addPart(@PathVariable Long id, @Valid @RequestBody AddPartRequest request) {
        return service.addPart(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a work order")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
