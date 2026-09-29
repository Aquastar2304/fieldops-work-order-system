package com.fieldops.repository;

import com.fieldops.domain.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {

    Optional<Site> findByCode(String code);

    boolean existsByCode(String code);
}
