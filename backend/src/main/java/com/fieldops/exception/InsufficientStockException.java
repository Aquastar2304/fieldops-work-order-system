package com.fieldops.exception;

/** Thrown when a part is consumed in a larger quantity than the store room holds. */
public class InsufficientStockException extends BusinessRuleException {

    public InsufficientStockException(String sku, int requested, int available) {
        super("Insufficient stock for part " + sku + ": requested " + requested + ", available " + available);
    }
}
