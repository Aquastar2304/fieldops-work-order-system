package com.fieldops.service;

import com.fieldops.domain.Site;
import com.fieldops.dto.SiteDto;
import com.fieldops.dto.SiteRequest;
import com.fieldops.exception.BusinessRuleException;
import com.fieldops.exception.NotFoundException;
import com.fieldops.mapper.WorkOrderMapper;
import com.fieldops.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SiteService {

    private final SiteRepository repository;
    private final WorkOrderMapper mapper;

    public SiteService(SiteRepository repository, WorkOrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<SiteDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SiteDto get(Long id) {
        return mapper.toDto(load(id));
    }

    public SiteDto create(SiteRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new BusinessRuleException("A site with code " + request.code() + " already exists");
        }
        Site site = Site.builder()
                .code(request.code())
                .name(request.name())
                .location(request.location())
                .build();
        return mapper.toDto(repository.save(site));
    }

    public SiteDto update(Long id, SiteRequest request) {
        Site site = load(id);
        repository.findByCode(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessRuleException("A site with code " + request.code() + " already exists");
                });
        site.setCode(request.code());
        site.setName(request.name());
        site.setLocation(request.location());
        return mapper.toDto(repository.save(site));
    }

    public void delete(Long id) {
        repository.delete(load(id));
    }

    private Site load(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("Site", id));
    }
}
