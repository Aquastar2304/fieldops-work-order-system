package com.fieldops.repository;

import com.fieldops.domain.Asset;
import com.fieldops.domain.Part;
import com.fieldops.domain.Site;
import com.fieldops.domain.Technician;
import com.fieldops.domain.WorkOrder;
import com.fieldops.domain.WorkOrderStatus;
import com.fieldops.dto.AddPartRequest;
import com.fieldops.dto.ChangeStatusRequest;
import com.fieldops.dto.CreateWorkOrderRequest;
import com.fieldops.dto.WorkOrderDetailDto;
import com.fieldops.exception.InsufficientStockException;
import com.fieldops.service.WorkOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the repository layer and the transactional stock decrement against a real PostgreSQL,
 * which is where Flyway, the JPA mappings and the check constraints actually meet.
 */
@DisplayName("Work order repository against PostgreSQL")
class WorkOrderRepositoryIT extends AbstractPostgresIT {

    @Autowired private WorkOrderRepository workOrderRepository;
    @Autowired private SiteRepository siteRepository;
    @Autowired private AssetRepository assetRepository;
    @Autowired private TechnicianRepository technicianRepository;
    @Autowired private PartRepository partRepository;
    @Autowired private WorkOrderService workOrderService;

    @Test
    void flywayLoadedTheSeedData() {
        assertThat(siteRepository.count()).isGreaterThanOrEqualTo(3);
        assertThat(assetRepository.count()).isGreaterThanOrEqualTo(5);
        assertThat(workOrderRepository.count()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void findByStatusReturnsOnlyThatStatus() {
        var page = workOrderRepository.findByStatus(WorkOrderStatus.OPEN, PageRequest.of(0, 20));

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allSatisfy(wo ->
                assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.OPEN));
    }

    @Test
    void findOverdueReturnsOnlyUnclosedWorkOrdersOlderThanTheThreshold() {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);

        List<WorkOrder> overdue = workOrderRepository.findOverdue(threshold);

        assertThat(overdue).isNotEmpty();
        assertThat(overdue).allSatisfy(wo -> {
            assertThat(wo.getStatus()).isNotEqualTo(WorkOrderStatus.CLOSED);
            assertThat(wo.getOpenedAt()).isBefore(threshold);
        });
        // Ordered oldest first so the report reads as a queue.
        assertThat(overdue).isSortedAccordingTo((a, b) -> a.getOpenedAt().compareTo(b.getOpenedAt()));
    }

    @Test
    void findWithDetailsByIdEagerlyLoadsTheAssociations() {
        WorkOrder seeded = workOrderRepository.findByReference("WO-000001").orElseThrow();

        WorkOrder loaded = workOrderRepository.findWithDetailsById(seeded.getId()).orElseThrow();

        // Would throw LazyInitializationException outside a session if the entity graph were ignored.
        assertThat(loaded.getAsset().getSite().getName()).isNotBlank();
        assertThat(loaded.getParts()).isNotEmpty();
        assertThat(loaded.getParts().getFirst().getPart().getSku()).isNotBlank();
    }

    @Test
    void referencesAreUniqueAcrossGeneratedWorkOrders() {
        Asset asset = assetRepository.findByTag("PMP-001").orElseThrow();

        WorkOrderDetailDto first = workOrderService.create(new CreateWorkOrderRequest(
                "Generated one", null, asset.getId(), null, WorkOrder.Priority.LOW));
        WorkOrderDetailDto second = workOrderService.create(new CreateWorkOrderRequest(
                "Generated two", null, asset.getId(), null, WorkOrder.Priority.LOW));

        assertThat(first.reference()).isNotEqualTo(second.reference());
        assertThat(first.reference()).startsWith("WO-");
    }

    @Test
    void consumingAPartCommitsBothTheLineAndTheStockDecrement() {
        Site site = siteRepository.findByCode("BKN-N").orElseThrow();
        Asset asset = assetRepository.findByTag("CMP-002").orElseThrow();
        Technician technician = technicianRepository.findByEmployeeNo("T-1001").orElseThrow();
        Part part = partRepository.findBySku("FLT-330").orElseThrow();
        int stockBefore = part.getQuantityInStock();
        assertThat(site).isNotNull();

        WorkOrderDetailDto created = workOrderService.create(new CreateWorkOrderRequest(
                "Filter change", null, asset.getId(), technician.getId(), WorkOrder.Priority.MEDIUM));
        workOrderService.changeStatus(created.id(), new ChangeStatusRequest(WorkOrderStatus.IN_PROGRESS));

        workOrderService.addPart(created.id(), new AddPartRequest(part.getId(), 5));

        assertThat(partRepository.findBySku("FLT-330").orElseThrow().getQuantityInStock())
                .isEqualTo(stockBefore - 5);
        assertThat(workOrderRepository.findWithDetailsById(created.id()).orElseThrow().getParts())
                .hasSize(1);
    }

    @Test
    void aStockShortfallLeavesNeitherThePartLineNorTheInventoryChanged() {
        Asset asset = assetRepository.findByTag("SEP-003").orElseThrow();
        Technician technician = technicianRepository.findByEmployeeNo("T-1003").orElseThrow();
        Part scarce = partRepository.findBySku("SNS-610").orElseThrow();
        int stockBefore = scarce.getQuantityInStock();

        WorkOrderDetailDto created = workOrderService.create(new CreateWorkOrderRequest(
                "Transmitter swap", null, asset.getId(), technician.getId(), WorkOrder.Priority.HIGH));
        workOrderService.changeStatus(created.id(), new ChangeStatusRequest(WorkOrderStatus.IN_PROGRESS));

        assertThatThrownBy(() ->
                        workOrderService.addPart(created.id(), new AddPartRequest(scarce.getId(), stockBefore + 1)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(partRepository.findBySku("SNS-610").orElseThrow().getQuantityInStock())
                .isEqualTo(stockBefore);
        assertThat(workOrderRepository.findWithDetailsById(created.id()).orElseThrow().getParts())
                .isEmpty();
    }

    @Test
    void theDatabaseAlsoRefusesToCloseAWorkOrderWithNoTechnician() {
        Asset asset = assetRepository.findByTag("GEN-005").orElseThrow();
        WorkOrder unassigned = WorkOrder.builder()
                .reference("WO-CHK-001")
                .title("Constraint probe")
                .asset(asset)
                .status(WorkOrderStatus.CLOSED)
                .priority(WorkOrder.Priority.LOW)
                .openedAt(Instant.now())
                .build();

        // The service guards this, and chk_closed_requires_technician guards it again at the table.
        assertThatThrownBy(() -> workOrderRepository.saveAndFlush(unassigned))
                .hasMessageContaining("chk_closed_requires_technician");
    }
}
