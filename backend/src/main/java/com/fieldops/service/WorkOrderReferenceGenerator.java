package com.fieldops.service;

/**
 * Supplies the human readable reference printed on a work order.
 *
 * <p>Extracted behind an interface so the service can be unit tested without a database sequence.
 */
public interface WorkOrderReferenceGenerator {

    String next();
}
