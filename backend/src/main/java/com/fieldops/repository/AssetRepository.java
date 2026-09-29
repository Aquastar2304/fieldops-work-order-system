package com.fieldops.repository;

import com.fieldops.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findBySiteId(Long siteId);

    Optional<Asset> findByTag(String tag);

    boolean existsByTag(String tag);
}
