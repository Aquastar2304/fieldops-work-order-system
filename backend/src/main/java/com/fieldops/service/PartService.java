package com.fieldops.service;

import com.fieldops.domain.Part;
import com.fieldops.dto.PartDto;
import com.fieldops.dto.PartRequest;
import com.fieldops.exception.BusinessRuleException;
import com.fieldops.exception.NotFoundException;
import com.fieldops.mapper.WorkOrderMapper;
import com.fieldops.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PartService {

    private final PartRepository repository;
    private final WorkOrderMapper mapper;

    public PartService(PartRepository repository, WorkOrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PartDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PartDto get(Long id) {
        return mapper.toDto(load(id));
    }

    public PartDto create(PartRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new BusinessRuleException("A part with SKU " + request.sku() + " already exists");
        }
        Part part = Part.builder()
                .sku(request.sku())
                .name(request.name())
                .quantityInStock(request.quantityInStock())
                .build();
        return mapper.toDto(repository.save(part));
    }

    public PartDto update(Long id, PartRequest request) {
        Part part = load(id);
        repository.findBySku(request.sku())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessRuleException("A part with SKU " + request.sku() + " already exists");
                });
        part.setSku(request.sku());
        part.setName(request.name());
        part.setQuantityInStock(request.quantityInStock());
        return mapper.toDto(repository.save(part));
    }

    public void delete(Long id) {
        repository.delete(load(id));
    }

    private Part load(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("Part", id));
    }
}
