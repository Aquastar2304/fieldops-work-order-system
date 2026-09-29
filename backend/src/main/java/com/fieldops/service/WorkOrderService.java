package com.fieldops.service;

import com.fieldops.domain.Asset;
import com.fieldops.domain.Part;
import com.fieldops.domain.Technician;
import com.fieldops.domain.WorkOrder;
import com.fieldops.domain.WorkOrderStatus;
import com.fieldops.dto.AddPartRequest;
import com.fieldops.dto.AssignTechnicianRequest;
import com.fieldops.dto.ChangeStatusRequest;
import com.fieldops.dto.CreateWorkOrderRequest;
import com.fieldops.dto.WorkOrderDetailDto;
import com.fieldops.dto.WorkOrderSummaryDto;
import com.fieldops.exception.InsufficientStockException;
import com.fieldops.exception.NotFoundException;
import com.fieldops.mapper.WorkOrderMapper;
import com.fieldops.repository.AssetRepository;
import com.fieldops.repository.PartRepository;
import com.fieldops.repository.TechnicianRepository;
import com.fieldops.repository.WorkOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final AssetRepository assetRepository;
    private final TechnicianRepository technicianRepository;
    private final PartRepository partRepository;
    private final WorkOrderReferenceGenerator referenceGenerator;
    private final WorkOrderMapper mapper;

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            AssetRepository assetRepository,
            TechnicianRepository technicianRepository,
            PartRepository partRepository,
            WorkOrderReferenceGenerator referenceGenerator,
            WorkOrderMapper mapper) {
        this.workOrderRepository = workOrderRepository;
        this.assetRepository = assetRepository;
        this.technicianRepository = technicianRepository;
        this.partRepository = partRepository;
        this.referenceGenerator = referenceGenerator;
        this.mapper = mapper;
    }

    public WorkOrderDetailDto create(CreateWorkOrderRequest request) {
        Asset asset = assetRepository
                .findById(request.assetId())
                .orElseThrow(() -> NotFoundException.of("Asset", request.assetId()));

        WorkOrder workOrder = WorkOrder.builder()
                .reference(referenceGenerator.next())
                .title(request.title())
                .description(request.description())
                .asset(asset)
                .priority(request.priority())
                .status(WorkOrderStatus.OPEN)
                .openedAt(Instant.now())
                .build();

        if (request.technicianId() != null) {
            workOrder.assignTechnician(loadTechnician(request.technicianId()));
        }

        return mapper.toDetail(workOrderRepository.save(workOrder));
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderSummaryDto> list(WorkOrderStatus status, Pageable pageable) {
        Page<WorkOrder> page = status == null
                ? workOrderRepository.findAll(pageable)
                : workOrderRepository.findByStatus(status, pageable);
        return page.map(mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public WorkOrderDetailDto get(Long id) {
        return mapper.toDetail(load(id));
    }

    @Transactional(readOnly = true)
    public List<WorkOrderSummaryDto> findOverdue(int olderThanDays) {
        Instant threshold = Instant.now().minus(Duration.ofDays(olderThanDays));
        return workOrderRepository.findOverdue(threshold).stream()
                .map(mapper::toSummary)
                .toList();
    }

    public WorkOrderDetailDto assignTechnician(Long id, AssignTechnicianRequest request) {
        WorkOrder workOrder = load(id);
        Technician technician = loadTechnician(request.technicianId());
        workOrder.assignTechnician(technician);
        return mapper.toDetail(workOrderRepository.save(workOrder));
    }

    public WorkOrderDetailDto changeStatus(Long id, ChangeStatusRequest request) {
        WorkOrder workOrder = load(id);
        workOrder.changeStatus(request.status());
        return mapper.toDetail(workOrderRepository.save(workOrder));
    }

    /**
     * Records a part against a work order and draws the same quantity out of inventory.
     *
     * <p>Both writes happen inside the one transaction opened by this method, so a stock shortfall
     * rolls the consumption back rather than leaving a part line with no matching decrement. The
     * part row is read under a pessimistic write lock so two concurrent callers cannot both observe
     * the same pre-decrement quantity.
     */
    public WorkOrderDetailDto addPart(Long id, AddPartRequest request) {
        WorkOrder workOrder = load(id);

        Part part = partRepository
                .findByIdForUpdate(request.partId())
                .orElseThrow(() -> NotFoundException.of("Part", request.partId()));

        if (part.getQuantityInStock() < request.quantity()) {
            throw new InsufficientStockException(
                    part.getSku(), request.quantity(), part.getQuantityInStock());
        }

        workOrder.addPart(part, request.quantity());
        part.setQuantityInStock(part.getQuantityInStock() - request.quantity());
        partRepository.save(part);

        return mapper.toDetail(workOrderRepository.save(workOrder));
    }

    public void delete(Long id) {
        WorkOrder workOrder = load(id);
        workOrderRepository.delete(workOrder);
    }

    private WorkOrder load(Long id) {
        return workOrderRepository
                .findWithDetailsById(id)
                .orElseThrow(() -> NotFoundException.of("Work order", id));
    }

    private Technician loadTechnician(Long technicianId) {
        return technicianRepository
                .findById(technicianId)
                .orElseThrow(() -> NotFoundException.of("Technician", technicianId));
    }
}
