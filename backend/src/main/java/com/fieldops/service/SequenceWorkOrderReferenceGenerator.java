package com.fieldops.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

/** Draws references from a Postgres sequence so they stay unique and gap tolerant. */
@Component
public class SequenceWorkOrderReferenceGenerator implements WorkOrderReferenceGenerator {

    private final EntityManager entityManager;

    public SequenceWorkOrderReferenceGenerator(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public String next() {
        Number value = (Number) entityManager
                .createNativeQuery("select nextval('work_order_reference_seq')")
                .getSingleResult();
        return String.format("WO-%06d", value.longValue());
    }
}
