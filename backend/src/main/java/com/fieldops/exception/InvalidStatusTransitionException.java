package com.fieldops.exception;

import com.fieldops.domain.WorkOrderStatus;

/** Thrown when a work order is asked to move to a status its lifecycle does not allow. */
public class InvalidStatusTransitionException extends BusinessRuleException {

    private final WorkOrderStatus from;
    private final WorkOrderStatus to;

    public InvalidStatusTransitionException(WorkOrderStatus from, WorkOrderStatus to) {
        super("Cannot move a work order from " + from + " to " + to
                + ". Allowed from " + from + ": " + from.allowedTargets());
        this.from = from;
        this.to = to;
    }

    public WorkOrderStatus getFrom() {
        return from;
    }

    public WorkOrderStatus getTo() {
        return to;
    }
}
