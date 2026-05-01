package com.deva.modules.dtos;

import java.time.Instant;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class DtosApplication {
    public static void main(String[] args) {
        SpringApplication.run(DtosApplication.class, args);
    }
}

record CreateOrderRequest(String sku, int quantity, String clientRequestId) {
}

record OrderResponse(String orderId, String sku, int quantity, String status, String acceptedAt) {
}

record OrderEntity(String id, String sku, int quantity, String status, int internalRiskScore, int internalCostCents) {
}

@RestController
class OrderController {
    @PostMapping("/api/orders")
    OrderResponse create(@RequestBody CreateOrderRequest request) {
        OrderEntity entity = new OrderEntity(
                "ord-" + Math.abs(request.clientRequestId().hashCode()),
                request.sku(),
                request.quantity(),
                "ACCEPTED",
                42,
                199);

        return new OrderResponse(
                entity.id(),
                entity.sku(),
                entity.quantity(),
                entity.status(),
                Instant.now().toString());
    }

    @PostMapping("/api/orders/debug-bad-contract")
    Map<String, Object> badContract(@RequestBody CreateOrderRequest request) {
        return Map.of(
                "sku", request.sku(),
                "internalRiskScore", 42,
                "lesson", "entities and internal fields must not leak to clients");
    }
}

