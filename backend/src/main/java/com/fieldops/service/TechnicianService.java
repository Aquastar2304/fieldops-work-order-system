package com.fieldops.service;

import com.fieldops.domain.Technician;
import com.fieldops.dto.TechnicianDto;
import com.fieldops.dto.TechnicianRequest;
import com.fieldops.exception.BusinessRuleException;
import com.fieldops.exception.NotFoundException;
import com.fieldops.mapper.WorkOrderMapper;
import com.fieldops.repository.TechnicianRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TechnicianService {

    private final TechnicianRepository repository;
    private final WorkOrderMapper mapper;

    public TechnicianService(TechnicianRepository repository, WorkOrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TechnicianDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TechnicianDto get(Long id) {
        return mapper.toDto(load(id));
    }

    public TechnicianDto create(TechnicianRequest request) {
        if (repository.existsByEmployeeNo(request.employeeNo())) {
            throw new BusinessRuleException(
                    "A technician with employee number " + request.employeeNo() + " already exists");
        }
        Technician technician = Technician.builder()
                .employeeNo(request.employeeNo())
                .fullName(request.fullName())
                .email(request.email())
                .specialty(request.specialty())
                .build();
        return mapper.toDto(repository.save(technician));
    }

    public TechnicianDto update(Long id, TechnicianRequest request) {
        Technician technician = load(id);
        repository.findByEmployeeNo(request.employeeNo())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessRuleException(
                            "A technician with employee number " + request.employeeNo() + " already exists");
                });
        technician.setEmployeeNo(request.employeeNo());
        technician.setFullName(request.fullName());
        technician.setEmail(request.email());
        technician.setSpecialty(request.specialty());
        return mapper.toDto(repository.save(technician));
    }

    public void delete(Long id) {
        repository.delete(load(id));
    }

    private Technician load(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("Technician", id));
    }
}
