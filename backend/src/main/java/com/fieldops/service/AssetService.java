package com.fieldops.service;

import com.fieldops.domain.Asset;
import com.fieldops.domain.Site;
import com.fieldops.dto.AssetDto;
import com.fieldops.dto.AssetRequest;
import com.fieldops.exception.BusinessRuleException;
import com.fieldops.exception.NotFoundException;
import com.fieldops.mapper.WorkOrderMapper;
import com.fieldops.repository.AssetRepository;
import com.fieldops.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AssetService {

    private final AssetRepository repository;
    private final SiteRepository siteRepository;
    private final WorkOrderMapper mapper;

    public AssetService(AssetRepository repository, SiteRepository siteRepository, WorkOrderMapper mapper) {
        this.repository = repository;
        this.siteRepository = siteRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AssetDto> findAll(Long siteId) {
        List<Asset> assets = siteId == null ? repository.findAll() : repository.findBySiteId(siteId);
        return assets.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AssetDto get(Long id) {
        return mapper.toDto(load(id));
    }

    public AssetDto create(AssetRequest request) {
        if (repository.existsByTag(request.tag())) {
            throw new BusinessRuleException("An asset with tag " + request.tag() + " already exists");
        }
        Asset asset = Asset.builder()
                .tag(request.tag())
                .name(request.name())
                .manufacturer(request.manufacturer())
                .model(request.model())
                .site(loadSite(request.siteId()))
                .build();
        return mapper.toDto(repository.save(asset));
    }

    public AssetDto update(Long id, AssetRequest request) {
        Asset asset = load(id);
        repository.findByTag(request.tag())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessRuleException("An asset with tag " + request.tag() + " already exists");
                });
        asset.setTag(request.tag());
        asset.setName(request.name());
        asset.setManufacturer(request.manufacturer());
        asset.setModel(request.model());
        asset.setSite(loadSite(request.siteId()));
        return mapper.toDto(repository.save(asset));
    }

    public void delete(Long id) {
        repository.delete(load(id));
    }

    private Asset load(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("Asset", id));
    }

    private Site loadSite(Long siteId) {
        return siteRepository.findById(siteId).orElseThrow(() -> NotFoundException.of("Site", siteId));
    }
}
