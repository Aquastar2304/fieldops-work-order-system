package com.fieldops.repository;

import com.fieldops.domain.WorkOrder;
import com.fieldops.domain.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    @EntityGraph(attributePaths = {"asset", "asset.site", "technician"})
    Page<WorkOrder> findByStatus(WorkOrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"asset", "asset.site", "technician"})
    Page<WorkOrder> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"asset", "asset.site", "technician", "parts", "parts.part"})
    Optional<WorkOrder> findWithDetailsById(Long id);

    Optional<WorkOrder> findByReference(String reference);

    /** Work orders still not closed that were opened before the given instant. */
    @Query("select w from WorkOrder w where w.status <> com.fieldops.domain.WorkOrderStatus.CLOSED "
            + "and w.openedAt < :threshold order by w.openedAt asc")
    List<WorkOrder> findOverdue(Instant threshold);

    long countByStatus(WorkOrderStatus status);
}
