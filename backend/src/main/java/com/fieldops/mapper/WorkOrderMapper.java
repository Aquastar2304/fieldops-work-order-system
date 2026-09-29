package com.fieldops.mapper;

import com.fieldops.domain.Asset;
import com.fieldops.domain.Part;
import com.fieldops.domain.Site;
import com.fieldops.domain.Technician;
import com.fieldops.domain.WorkOrder;
import com.fieldops.dto.AssetDto;
import com.fieldops.dto.PartDto;
import com.fieldops.dto.SiteDto;
import com.fieldops.dto.TechnicianDto;
import com.fieldops.dto.WorkOrderDetailDto;
import com.fieldops.dto.WorkOrderPartDto;
import com.fieldops.dto.WorkOrderSummaryDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns entities into the DTOs the API exposes.
 *
 * <p>Deliberately hand written and dependency free so it can be used directly in unit tests without
 * a Spring context.
 */
@Component
public class WorkOrderMapper {

    public SiteDto toDto(Site site) {
        if (site == null) {
            return null;
        }
        return new SiteDto(site.getId(), site.getCode(), site.getName(), site.getLocation());
    }

    public AssetDto toDto(Asset asset) {
        if (asset == null) {
            return null;
        }
        Site site = asset.getSite();
        return new AssetDto(
                asset.getId(),
                asset.getTag(),
                asset.getName(),
                asset.getManufacturer(),
                asset.getModel(),
                site != null ? site.getId() : null,
                site != null ? site.getName() : null);
    }

    public TechnicianDto toDto(Technician technician) {
        if (technician == null) {
            return null;
        }
        return new TechnicianDto(
                technician.getId(),
                technician.getEmployeeNo(),
                technician.getFullName(),
                technician.getEmail(),
                technician.getSpecialty());
    }

    public PartDto toDto(Part part) {
        if (part == null) {
            return null;
        }
        return new PartDto(part.getId(), part.getSku(), part.getName(), part.getQuantityInStock());
    }

    public WorkOrderSummaryDto toSummary(WorkOrder workOrder) {
        Technician technician = workOrder.getTechnician();
        Asset asset = workOrder.getAsset();
        return new WorkOrderSummaryDto(
                workOrder.getId(),
                workOrder.getReference(),
                workOrder.getTitle(),
                workOrder.getStatus(),
                workOrder.getPriority(),
                asset != null ? asset.getTag() : null,
                technician != null ? technician.getFullName() : null,
                workOrder.getOpenedAt(),
                workOrder.getClosedAt());
    }

    public WorkOrderDetailDto toDetail(WorkOrder workOrder) {
        List<WorkOrderPartDto> lines = workOrder.getParts().stream()
                .map(line -> new WorkOrderPartDto(
                        line.getPart().getId(),
                        line.getPart().getSku(),
                        line.getPart().getName(),
                        line.getQuantity()))
                .toList();

        return new WorkOrderDetailDto(
                workOrder.getId(),
                workOrder.getReference(),
                workOrder.getTitle(),
                workOrder.getDescription(),
                workOrder.getStatus(),
                workOrder.getPriority(),
                toDto(workOrder.getAsset()),
                toDto(workOrder.getTechnician()),
                workOrder.getOpenedAt(),
                workOrder.getClosedAt(),
                lines,
                workOrder.getStatus().allowedTargets(),
                workOrder.getVersion());
    }
}
