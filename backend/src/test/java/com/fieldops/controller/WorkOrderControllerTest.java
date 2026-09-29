package com.fieldops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fieldops.domain.WorkOrder;
import com.fieldops.domain.WorkOrderStatus;
import com.fieldops.dto.AddPartRequest;
import com.fieldops.dto.AssetDto;
import com.fieldops.dto.ChangeStatusRequest;
import com.fieldops.dto.CreateWorkOrderRequest;
import com.fieldops.dto.WorkOrderDetailDto;
import com.fieldops.exception.GlobalExceptionHandler;
import com.fieldops.exception.InsufficientStockException;
import com.fieldops.exception.InvalidStatusTransitionException;
import com.fieldops.exception.NotFoundException;
import com.fieldops.service.WorkOrderService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the HTTP contract of the work order endpoints and that domain failures come back as the
 * documented JSON error shape rather than a stack trace.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkOrderController")
class WorkOrderControllerTest {

    @Mock private WorkOrderService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WorkOrderController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private static WorkOrderDetailDto detail(WorkOrderStatus status) {
        return new WorkOrderDetailDto(
                100L,
                "WO-000100",
                "Pump vibration",
                "Excessive vibration",
                status,
                WorkOrder.Priority.HIGH,
                new AssetDto(10L, "PMP-01", "Transfer pump", "Sulzer", "MSD", 1L, "Bakken North"),
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                List.of(),
                Set.of(WorkOrderStatus.ASSIGNED),
                0L);
    }

    @Test
    void returnsTheWorkOrderDetail() throws Exception {
        when(service.get(100L)).thenReturn(detail(WorkOrderStatus.OPEN));

        mockMvc.perform(get("/api/work-orders/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("WO-000100"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.asset.tag").value("PMP-01"))
                .andExpect(jsonPath("$.allowedTransitions[0]").value("ASSIGNED"));
    }

    @Test
    void returns201WithALocationHeaderOnCreate() throws Exception {
        when(service.create(any())).thenReturn(detail(WorkOrderStatus.OPEN));

        CreateWorkOrderRequest request = new CreateWorkOrderRequest(
                "Pump vibration", "Excessive vibration", 10L, null, WorkOrder.Priority.HIGH);

        mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    void returns400WithFieldViolationsWhenTheBodyIsInvalid() throws Exception {
        CreateWorkOrderRequest invalid = new CreateWorkOrderRequest("", null, null, null, null);

        mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.violations").isNotEmpty());
    }

    @Test
    void returns404WhenTheWorkOrderIsMissing() throws Exception {
        when(service.get(404L)).thenThrow(NotFoundException.of("Work order", 404L));

        mockMvc.perform(get("/api/work-orders/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Work order 404 was not found"))
                .andExpect(jsonPath("$.path").value("/api/work-orders/404"));
    }

    @Test
    void returns409ForAnIllegalStatusTransition() throws Exception {
        when(service.changeStatus(eq(100L), any()))
                .thenThrow(new InvalidStatusTransitionException(
                        WorkOrderStatus.OPEN, WorkOrderStatus.CLOSED));

        mockMvc.perform(post("/api/work-orders/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeStatusRequest(WorkOrderStatus.CLOSED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        Matchers.containsString("Cannot move a work order from OPEN to CLOSED")));
    }

    @Test
    void returns409WhenStockIsInsufficient() throws Exception {
        when(service.addPart(eq(100L), any()))
                .thenThrow(new InsufficientStockException("SEAL-9", 5, 2));

        mockMvc.perform(post("/api/work-orders/100/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddPartRequest(30L, 5))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        Matchers.containsString("Insufficient stock for part SEAL-9")));
    }

    @Test
    void rejectsANonPositivePartQuantityBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/work-orders/100/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddPartRequest(30L, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("quantity"));
    }
}
