package com.deva.learn.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class InventoryUnavailableException extends ApiException {
    public InventoryUnavailableException(String sku, int requestNumber) {
        super(
                HttpStatus.SERVICE_UNAVAILABLE,
                "INVENTORY_UNAVAILABLE",
                "Inventory service is temporarily unavailable",
                Map.of(
                        "sku", sku,
                        "requestNumber", requestNumber));
    }
}
