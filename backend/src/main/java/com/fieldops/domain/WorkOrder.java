package com.fieldops.domain;

import com.fieldops.exception.BusinessRuleException;
import com.fieldops.exception.InvalidStatusTransitionException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", nullable = false, unique = true, length = 40)
    private String reference;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WorkOrderStatus status = WorkOrderStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @Column(name = "opened_at", nullable = false)
    @Builder.Default
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkOrderPart> parts = new ArrayList<>();

    /** Guards against two supervisors editing the same work order concurrently. */
    @Version
    private Long version;

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * Moves the work order to {@code target}, enforcing the lifecycle rules.
     *
     * @throws InvalidStatusTransitionException if the move is not part of the lifecycle
     * @throws BusinessRuleException if closing without an assigned technician
     */
    public void changeStatus(WorkOrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(status, target);
        }
        if (target == WorkOrderStatus.CLOSED && technician == null) {
            throw new BusinessRuleException("A work order cannot be closed without an assigned technician");
        }
        this.status = target;
        this.closedAt = target == WorkOrderStatus.CLOSED ? Instant.now() : null;
        if (target == WorkOrderStatus.OPEN) {
            this.technician = null;
        }
    }

    /**
     * Assigns a technician. A closed work order is immutable, and assigning from OPEN also advances
     * the status to ASSIGNED because those two facts are the same event.
     */
    public void assignTechnician(Technician newTechnician) {
        if (newTechnician == null) {
            throw new BusinessRuleException("Technician must not be null");
        }
        if (status == WorkOrderStatus.CLOSED) {
            throw new BusinessRuleException("A closed work order cannot be reassigned");
        }
        this.technician = newTechnician;
        if (status == WorkOrderStatus.OPEN) {
            this.status = WorkOrderStatus.ASSIGNED;
        }
    }

    /** Adds a consumed part line, merging with an existing line for the same part. */
    public WorkOrderPart addPart(Part part, int quantity) {
        if (status == WorkOrderStatus.CLOSED) {
            throw new BusinessRuleException("Parts cannot be added to a closed work order");
        }
        if (quantity <= 0) {
            throw new BusinessRuleException("Quantity must be greater than zero");
        }
        WorkOrderPart existing = parts.stream()
                .filter(line -> line.getPart().getId() != null && line.getPart().getId().equals(part.getId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            return existing;
        }
        WorkOrderPart line = WorkOrderPart.builder()
                .workOrder(this)
                .part(part)
                .quantity(quantity)
                .build();
        parts.add(line);
        return line;
    }
}
