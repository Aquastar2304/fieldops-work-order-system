package com.fieldops.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle of a work order.
 *
 * <p>The allowed moves are declared here rather than scattered through the service so that the
 * lifecycle is readable in one place and can be unit tested without touching the database.
 */
public enum WorkOrderStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    CLOSED;

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED = Map.of(
            OPEN, EnumSet.of(ASSIGNED),
            ASSIGNED, EnumSet.of(IN_PROGRESS, OPEN),
            IN_PROGRESS, EnumSet.of(CLOSED, ASSIGNED),
            CLOSED, Collections.emptySet());

    public boolean canTransitionTo(WorkOrderStatus target) {
        return target != null && ALLOWED.get(this).contains(target);
    }

    public Set<WorkOrderStatus> allowedTargets() {
        return Collections.unmodifiableSet(ALLOWED.get(this));
    }

    public boolean isTerminal() {
        return ALLOWED.get(this).isEmpty();
    }
}
