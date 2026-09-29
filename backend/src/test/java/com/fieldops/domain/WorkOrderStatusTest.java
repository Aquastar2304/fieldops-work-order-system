package com.fieldops.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Work order lifecycle")
class WorkOrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
        "OPEN, ASSIGNED",
        "ASSIGNED, IN_PROGRESS",
        "ASSIGNED, OPEN",
        "IN_PROGRESS, CLOSED",
        "IN_PROGRESS, ASSIGNED"
    })
    void allowsMovesAlongTheLifecycle(WorkOrderStatus from, WorkOrderStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
        "OPEN, IN_PROGRESS",
        "OPEN, CLOSED",
        "OPEN, OPEN",
        "ASSIGNED, CLOSED",
        "ASSIGNED, ASSIGNED",
        "IN_PROGRESS, OPEN",
        "CLOSED, OPEN",
        "CLOSED, ASSIGNED",
        "CLOSED, IN_PROGRESS",
        "CLOSED, CLOSED"
    })
    void rejectsMovesThatSkipOrReopenTheLifecycle(WorkOrderStatus from, WorkOrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(WorkOrderStatus.class)
    void neverAllowsATransitionToNull(WorkOrderStatus from) {
        assertThat(from.canTransitionTo(null)).isFalse();
    }

    @Test
    void closedIsTheOnlyTerminalStatus() {
        assertThat(WorkOrderStatus.CLOSED.isTerminal()).isTrue();
        assertThat(WorkOrderStatus.OPEN.isTerminal()).isFalse();
        assertThat(WorkOrderStatus.ASSIGNED.isTerminal()).isFalse();
        assertThat(WorkOrderStatus.IN_PROGRESS.isTerminal()).isFalse();
    }

    @Test
    void exposesAllowedTargetsForTheUi() {
        assertThat(WorkOrderStatus.ASSIGNED.allowedTargets())
                .containsExactlyInAnyOrder(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.OPEN);
        assertThat(WorkOrderStatus.CLOSED.allowedTargets()).isEmpty();
    }
}
