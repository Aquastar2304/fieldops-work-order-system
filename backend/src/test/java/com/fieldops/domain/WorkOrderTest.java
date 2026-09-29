package com.fieldops.domain;

import com.fieldops.exception.BusinessRuleException;
import com.fieldops.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WorkOrder domain rules")
class WorkOrderTest {

    private static Technician technician() {
        return Technician.builder().id(7L).employeeNo("T-007").fullName("Dana Okoro").build();
    }

    private static Part part(long id, String sku) {
        return Part.builder().id(id).sku(sku).name("Seal kit").quantityInStock(10).build();
    }

    private static WorkOrder openWorkOrder() {
        return WorkOrder.builder()
                .id(1L)
                .reference("WO-000001")
                .title("Pump vibration")
                .asset(Asset.builder().id(1L).tag("A-1").build())
                .status(WorkOrderStatus.OPEN)
                .build();
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        void rejectsATransitionOutsideTheLifecycle() {
            WorkOrder wo = openWorkOrder();

            assertThatThrownBy(() -> wo.changeStatus(WorkOrderStatus.IN_PROGRESS))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("OPEN")
                    .hasMessageContaining("IN_PROGRESS");

            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
        }

        @Test
        void refusesToCloseWithoutAnAssignedTechnician() {
            WorkOrder wo = openWorkOrder();
            wo.setStatus(WorkOrderStatus.IN_PROGRESS);
            wo.setTechnician(null);

            assertThatThrownBy(() -> wo.changeStatus(WorkOrderStatus.CLOSED))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("cannot be closed without an assigned technician");

            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
            assertThat(wo.getClosedAt()).isNull();
        }

        @Test
        void closesAndStampsClosedAtWhenATechnicianIsAssigned() {
            WorkOrder wo = openWorkOrder();
            wo.assignTechnician(technician());
            wo.changeStatus(WorkOrderStatus.IN_PROGRESS);

            wo.changeStatus(WorkOrderStatus.CLOSED);

            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.CLOSED);
            assertThat(wo.getClosedAt()).isNotNull();
        }

        @Test
        void unassigningBackToOpenClearsTheTechnician() {
            WorkOrder wo = openWorkOrder();
            wo.assignTechnician(technician());

            wo.changeStatus(WorkOrderStatus.OPEN);

            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
            assertThat(wo.getTechnician()).isNull();
        }
    }

    @Nested
    @DisplayName("assignTechnician")
    class AssignTechnician {

        @Test
        void movesAnOpenWorkOrderToAssigned() {
            WorkOrder wo = openWorkOrder();

            wo.assignTechnician(technician());

            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.ASSIGNED);
            assertThat(wo.getTechnician().getEmployeeNo()).isEqualTo("T-007");
        }

        @Test
        void reassigningAnInProgressWorkOrderKeepsItInProgress() {
            WorkOrder wo = openWorkOrder();
            wo.assignTechnician(technician());
            wo.changeStatus(WorkOrderStatus.IN_PROGRESS);

            Technician other = Technician.builder().id(9L).employeeNo("T-009").fullName("Sam Reyes").build();
            wo.assignTechnician(other);

            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
            assertThat(wo.getTechnician().getEmployeeNo()).isEqualTo("T-009");
        }

        @Test
        void refusesToReassignAClosedWorkOrder() {
            WorkOrder wo = openWorkOrder();
            wo.assignTechnician(technician());
            wo.changeStatus(WorkOrderStatus.IN_PROGRESS);
            wo.changeStatus(WorkOrderStatus.CLOSED);

            assertThatThrownBy(() -> wo.assignTechnician(technician()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("closed work order cannot be reassigned");
        }
    }

    @Nested
    @DisplayName("addPart")
    class AddPart {

        @Test
        void addsANewLine() {
            WorkOrder wo = openWorkOrder();

            WorkOrderPart line = wo.addPart(part(3L, "SKU-3"), 2);

            assertThat(wo.getParts()).hasSize(1);
            assertThat(line.getQuantity()).isEqualTo(2);
            assertThat(line.getWorkOrder()).isSameAs(wo);
        }

        @Test
        void mergesRepeatedUseOfTheSamePartIntoOneLine() {
            WorkOrder wo = openWorkOrder();
            Part seal = part(3L, "SKU-3");

            wo.addPart(seal, 2);
            WorkOrderPart merged = wo.addPart(seal, 3);

            assertThat(wo.getParts()).hasSize(1);
            assertThat(merged.getQuantity()).isEqualTo(5);
        }

        @Test
        void rejectsANonPositiveQuantity() {
            WorkOrder wo = openWorkOrder();

            assertThatThrownBy(() -> wo.addPart(part(3L, "SKU-3"), 0))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        void refusesToAddPartsToAClosedWorkOrder() {
            WorkOrder wo = openWorkOrder();
            wo.assignTechnician(technician());
            wo.changeStatus(WorkOrderStatus.IN_PROGRESS);
            wo.changeStatus(WorkOrderStatus.CLOSED);

            assertThatThrownBy(() -> wo.addPart(part(3L, "SKU-3"), 1))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("closed work order");
        }
    }
}
