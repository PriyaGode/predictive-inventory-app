package com.shop.product.circuitbreaker;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * SYNCHRONOUS REST communication from product-service to other services.
 *
 * Circuit Breaker states:
 *   CLOSED   → Normal operation, calls pass through
 *   OPEN     → Too many failures, calls rejected immediately (fast fail)
 *   HALF-OPEN → Testing if service recovered, allows limited calls
 *
 * Retry: 3 attempts with exponential backoff (500ms, 1s, 2s)
 * Bulkhead: Max 10 concurrent calls, isolates thread pool
 */
@Component
public class ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @CircuitBreaker(name = "orderService", fallbackMethod = "orderServiceFallback")
    @Retry(name = "orderService", fallbackMethod = "orderServiceFallback")
    @Bulkhead(name = "orderService", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "orderServiceFallback")
    public Map<String, Object> getOrderStats(String email) {
        log.info("🔗 [SYNC-REST] Calling order-service for email={}", email);
        Map response = restTemplate.getForObject("http://localhost:8082/api/orders/customer/" + email, Map.class);
        log.info("✅ [SYNC-REST] Order-service response received");
        return response;
    }

    public Map<String, Object> orderServiceFallback(String email, Exception ex) {
        log.warn("⚡ [CIRCUIT BREAKER] Order-service fallback for email={} | reason={}", email, ex.getMessage());
        return Map.of("orders", java.util.List.of(), "fallback", true,
                "message", "Order service unavailable — using fallback");
    }
}
