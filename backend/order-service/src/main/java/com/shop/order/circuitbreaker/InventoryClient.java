package com.shop.order.circuitbreaker;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * SYNCHRONOUS REST communication to inventory-service.
 *
 * Resilience4j patterns applied:
 * - @CircuitBreaker : Opens after 5 failures, stays open 10s, then half-open to test
 * - @Retry          : Retries up to 3 times with 500ms wait on failure
 * - @Bulkhead       : Max 5 concurrent calls, rejects extras to isolate thread pool
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);
    private static final String INVENTORY_URL = "http://localhost:8083/api/inventory/product/";

    private final RestTemplate restTemplate;

    public InventoryClient() {
        this.restTemplate = new RestTemplate();
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "inventoryFallback")
    @Retry(name = "inventoryService", fallbackMethod = "inventoryFallback")
    @Bulkhead(name = "inventoryService", fallbackMethod = "inventoryFallback")
    public Map<String, Object> checkStock(Long productId) {
        log.info("🔗 [SYNC-REST] Calling inventory-service for productId={}", productId);
        Map response = restTemplate.getForObject(INVENTORY_URL + productId, Map.class);
        log.info("✅ [SYNC-REST] Inventory response for productId={} → {}", productId, response);
        return response;
    }

    // Fallback when circuit is OPEN or all retries exhausted
    public Map<String, Object> inventoryFallback(Long productId, Exception ex) {
        log.warn("⚡ [CIRCUIT BREAKER] Fallback triggered for productId={} | reason={}", productId, ex.getMessage());
        return Map.of(
                "productId", productId,
                "status", "UNKNOWN",
                "quantity", -1,
                "fallback", true,
                "message", "Inventory service unavailable — using fallback"
        );
    }
}
