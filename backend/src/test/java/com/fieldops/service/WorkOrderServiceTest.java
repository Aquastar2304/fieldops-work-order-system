package com.fieldops.service;

import com.fieldops.domain.Asset;
import com.fieldops.domain.Part;
import com.fieldops.domain.Site;
import com.fieldops.domain.Technician;
import com.fieldops.domain.WorkOrder;
import com.fieldops.domain.WorkOrderStatus;
import com.fieldops.dto.AddPartRequest;
import com.fieldops.dto.AssignTechnicianRequest;
import com.fieldops.dto.ChangeStatusRequest;
import com.fieldops.dto.CreateWorkOrderRequest;
import com.fieldops.dto.WorkOrderDetailDto;
import com.fieldops.exception.BusinessRuleException;
import com.fieldops.exception.InsufficientStockException;
import com.fieldops.exception.InvalidStatusTransitionException;
import com.fieldops.exception.NotFoundException;
import com.fieldops.mapper.WorkOrderMapper;
import com.fieldops.repository.AssetRepository;
import com.fieldops.repository.PartRepository;
import com.fieldops.repository.TechnicianRepository;
import com.fieldops.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkOrderService")
class WorkOrderServiceTest {

    @Mock private WorkOrderRepository workOrderRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private TechnicianRepository technicianRepository;
    @Mock private PartRepository partRepository;
    @Mock private WorkOrderReferenceGenerator referenceGenerator;

    private WorkOrderService service;

    private Asset asset;
    private Technician technician;

    @BeforeEach
    void setUp() {
        service = new WorkOrderService(
                workOrderRepository,
                assetRepository,
                technicianRepository,
                partRepository,
                referenceGenerator,
                new WorkOrderMapper());

        Site site = Site.builder().id(1L).code("S-1").name("Bakken North").build();
        asset = Asset.builder().id(10L).tag("PMP-01").name("Transfer pump").site(site).build();
        technician = Technician.builder().id(20L).employeeNo("T-007").fullName("Dana Okoro").build();
    }

    /** Makes save() behave like a database that hands back what it was given. */
    private void echoOnSave() {
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private WorkOrder existing(WorkOrderStatus status) {
        WorkOrder wo = WorkOrder.builder()
                .id(100L)
                .reference("WO-000100")
                .title("Pump vibration")
                .asset(asset)
                .status(status)
                .build();
        if (status != WorkOrderStatus.OPEN) {
            wo.setTechnician(technician);
        }
        return wo;
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void createsAnOpenWorkOrderAgainstTheGivenAsset() {
            when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
            when(referenceGenerator.next()).thenReturn("WO-000001");
            echoOnSave();

            WorkOrderDetailDto result = service.create(new CreateWorkOrderRequest(
                    "Pump vibration", "Excessive vibration on start-up", 10L, null, WorkOrder.Priority.HIGH));

            assertThat(result.status()).isEqualTo(WorkOrderStatus.OPEN);
            assertThat(result.reference()).isEqualTo("WO-000001");
            assertThat(result.asset().tag()).isEqualTo("PMP-01");
            assertThat(result.technician()).isNull();
            assertThat(result.allowedTransitions()).containsExactly(WorkOrderStatus.ASSIGNED);
        }

        @Test
        void createsDirectlyAsAssignedWhenATechnicianIsSupplied() {
            when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
            when(technicianRepository.findById(20L)).thenReturn(Optional.of(technician));
            when(referenceGenerator.next()).thenReturn("WO-000002");
            echoOnSave();

            WorkOrderDetailDto result = service.create(
                    new CreateWorkOrderRequest("Seal leak", null, 10L, 20L, WorkOrder.Priority.MEDIUM));

            assertThat(result.status()).isEqualTo(WorkOrderStatus.ASSIGNED);
            assertThat(result.technician().employeeNo()).isEqualTo("T-007");
        }

        @Test
        void failsWhenTheAssetDoesNotExist() {
            when(assetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(
                            new CreateWorkOrderRequest("Ghost", null, 99L, null, WorkOrder.Priority.LOW)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Asset");

            verify(workOrderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("assignTechnician")
    class Assign {

        @Test
        void assignsAndMovesTheWorkOrderToAssigned() {
            WorkOrder wo = existing(WorkOrderStatus.OPEN);
            when(workOrderRepository.findWithDetailsById(100L)).thenReturn(Optional.of(wo));
            when(technicianRepository.findById(20L)).thenReturn(Optional.of(technician));
            echoOnSave();

            WorkOrderDetailDto result = service.assignTechnician(100L, new AssignTechnicianRequest(20L));

            assertThat(result.status()).isEqualTo(WorkOrderStatus.ASSIGNED);
            assertThat(result.technician().fullName()).isEqualTo("Dana Okoro");
        }

        @Test
        void failsWhenTheTechnicianDoesNotExist() {
            when(workOrderRepository.findWithDetailsById(100L))
                    .thenReturn(Optional.of(existing(WorkOrderStatus.OPEN)));
            when(technicianRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignTechnician(100L, new AssignTechnicianRequest(404L)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Technician");

            verify(workOrderRepository, never()).save(any());
        }

        @Test
        void failsWhenTheWorkOrderDoesNotExist() {
            when(workOrderRepository.findWithDetailsById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignTechnician(404L, new AssignTechnicianRequest(20L)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Work order");
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        void rejectsATransitionOutsideTheLifecycle() {
            when(workOrderRepository.findWithDetailsById(100L))
                    .thenReturn(Optional.of(existing(WorkOrderStatus.OPEN)));

            assertThatThrownBy(() -> service.changeStatus(100L, new ChangeStatusRequest(WorkOrderStatus.CLOSED)))
                    .isInstanceOf(InvalidStatusTransitionException.class);

            verify(workOrderRepository, never()).save(any());
        }

        @Test
        void refusesToCloseAWorkOrderWithNoTechnician() {
            WorkOrder wo = existing(WorkOrderStatus.IN_PROGRESS);
            wo.setTechnician(null);
            when(workOrderRepository.findWithDetailsById(100L)).thenReturn(Optional.of(wo));

            assertThatThrownBy(() -> service.changeStatus(100L, new ChangeStatusRequest(WorkOrderStatus.CLOSED)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("cannot be closed without an assigned technician");

            verify(workOrderRepository, never()).save(any());
        }

        @Test
        void closesAWorkOrderThatHasATechnician() {
            when(workOrderRepository.findWithDetailsById(100L))
                    .thenReturn(Optional.of(existing(WorkOrderStatus.IN_PROGRESS)));
            echoOnSave();

            WorkOrderDetailDto result =
                    service.changeStatus(100L, new ChangeStatusRequest(WorkOrderStatus.CLOSED));

            assertThat(result.status()).isEqualTo(WorkOrderStatus.CLOSED);
            assertThat(result.closedAt()).isNotNull();
            assertThat(result.allowedTransitions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("addPart")
    class AddPart {

        @Test
        void decrementsInventoryByTheConsumedQuantity() {
            WorkOrder wo = existing(WorkOrderStatus.IN_PROGRESS);
            Part seal = Part.builder().id(30L).sku("SEAL-9").name("Seal kit").quantityInStock(10).build();
            when(workOrderRepository.findWithDetailsById(100L)).thenReturn(Optional.of(wo));
            when(partRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(seal));
            echoOnSave();

            WorkOrderDetailDto result = service.addPart(100L, new AddPartRequest(30L, 4));

            assertThat(seal.getQuantityInStock()).isEqualTo(6);
            assertThat(result.parts()).singleElement().satisfies(line -> {
                assertThat(line.sku()).isEqualTo("SEAL-9");
                assertThat(line.quantity()).isEqualTo(4);
            });

            ArgumentCaptor<Part> saved = ArgumentCaptor.forClass(Part.class);
            verify(partRepository).save(saved.capture());
            assertThat(saved.getValue().getQuantityInStock()).isEqualTo(6);
        }

        @Test
        void failsAndLeavesStockUntouchedWhenThereIsNotEnough() {
            WorkOrder wo = existing(WorkOrderStatus.IN_PROGRESS);
            Part seal = Part.builder().id(30L).sku("SEAL-9").name("Seal kit").quantityInStock(2).build();
            when(workOrderRepository.findWithDetailsById(100L)).thenReturn(Optional.of(wo));
            when(partRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(seal));

            assertThatThrownBy(() -> service.addPart(100L, new AddPartRequest(30L, 5)))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("SEAL-9")
                    .hasMessageContaining("requested 5")
                    .hasMessageContaining("available 2");

            assertThat(seal.getQuantityInStock()).isEqualTo(2);
            assertThat(wo.getParts()).isEmpty();
            verify(partRepository, never()).save(any());
            verify(workOrderRepository, never()).save(any());
        }

        @Test
        void allowsConsumingExactlyTheRemainingStock() {
            WorkOrder wo = existing(WorkOrderStatus.IN_PROGRESS);
            Part seal = Part.builder().id(30L).sku("SEAL-9").name("Seal kit").quantityInStock(3).build();
            when(workOrderRepository.findWithDetailsById(100L)).thenReturn(Optional.of(wo));
            when(partRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(seal));
            echoOnSave();

            service.addPart(100L, new AddPartRequest(30L, 3));

            assertThat(seal.getQuantityInStock()).isZero();
        }

        @Test
        void takesARowLockOnThePartRatherThanAPlainRead() {
            WorkOrder wo = existing(WorkOrderStatus.IN_PROGRESS);
            Part seal = Part.builder().id(30L).sku("SEAL-9").name("Seal kit").quantityInStock(10).build();
            when(workOrderRepository.findWithDetailsById(100L)).thenReturn(Optional.of(wo));
            when(partRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(seal));
            echoOnSave();

            service.addPart(100L, new AddPartRequest(30L, 1));

            verify(partRepository).findByIdForUpdate(30L);
            verify(partRepository, never()).findById(anyLong());
        }

        @Test
        void failsWhenThePartDoesNotExist() {
            when(workOrderRepository.findWithDetailsById(100L))
                    .thenReturn(Optional.of(existing(WorkOrderStatus.IN_PROGRESS)));
            when(partRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addPart(100L, new AddPartRequest(404L, 1)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Part");
        }
    }
}
