package com.fieldops.repository;

import com.fieldops.domain.Part;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {

    Optional<Part> findBySku(String sku);

    boolean existsBySku(String sku);

    /**
     * Reads a part for update. Stock decrements take a pessimistic write lock so two concurrent
     * work orders cannot both pass the "enough stock" check against the same row.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Part p where p.id = :id")
    Optional<Part> findByIdForUpdate(Long id);
}
