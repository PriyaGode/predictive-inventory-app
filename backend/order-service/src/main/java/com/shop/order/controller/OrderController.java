package com.shop.order.controller;

import com.shop.order.circuitbreaker.InventoryClient;
import com.shop.order.model.Order;
import com.shop.order.model.OrderRequest;
import com.shop.order.saga.SagaChoreographyProducer;
import com.shop.order.saga.SagaEvent;
import com.shop.order.saga.SagaOrchestrator;
import com.shop.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService service;
    private final InventoryClient inventoryClient;
    private final SagaChoreographyProducer choreographyProducer;
    private final SagaOrchestrator orchestrator;

    public OrderController(OrderService service, InventoryClient inventoryClient,
                           SagaChoreographyProducer choreographyProducer, SagaOrchestrator orchestrator) {
        this.service = service;
        this.inventoryClient = inventoryClient;
        this.choreographyProducer = choreographyProducer;
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public List<Order> getAll() { return service.getAllOrders(); }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getOrderById(id));
    }

    @GetMapping("/customer")
    public List<Order> getByEmail(@RequestParam String email) {
        return service.getOrdersByEmail(email);
    }

    /**
     * ASYNC (Kafka) + SAGA CHOREOGRAPHY order placement:
     * 1. Sync REST check stock via Circuit Breaker
     * 2. Save order as PENDING
     * 3. Publish ORDER_CREATED saga event → inventory listens and reserves stock
     * 4. Inventory replies STOCK_RESERVED/STOCK_FAILED → SagaChoreographyConsumer handles
     */
    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        // Step 1: SYNCHRONOUS stock check with Circuit Breaker + Retry + Bulkhead
        Long productId = request.getItems().get(0).getProductId();
        Map<String, Object> stockInfo = inventoryClient.checkStock(productId);
        log.info("📦 [ORDER] Stock check result: {}", stockInfo);

        if (Boolean.TRUE.equals(stockInfo.get("fallback"))) {
            log.warn("⚡ [ORDER] Inventory unavailable, proceeding with saga anyway");
        }

        // Step 2: Save order as PENDING
        Order order = service.placeOrder(request);

        // Step 3: Start SAGA CHOREOGRAPHY — publish ORDER_CREATED event
        SagaEvent sagaEvent = new SagaEvent("ORDER_CREATED", order.getId(), productId,
                request.getItems().get(0).getProductName(),
                request.getItems().get(0).getQuantity(),
                order.getTotalAmount(), order.getCustomerEmail());
        choreographyProducer.publishOrderCreated(sagaEvent);

        // Step 4: Also start SAGA ORCHESTRATION flow
        orchestrator.startOrderSaga(order, productId, request.getItems().get(0).getQuantity());

        return ResponseEntity.ok(Map.of(
                "order", order,
                "sagaId", sagaEvent.getSagaId(),
                "sagaType", "CHOREOGRAPHY + ORCHESTRATION",
                "stockCheck", stockInfo
        ));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(service.updateStatus(id, status));
    }

    // Endpoint to test Circuit Breaker state
    @GetMapping("/circuit-breaker/stock/{productId}")
    public ResponseEntity<Map<String, Object>> checkStockWithCircuitBreaker(@PathVariable Long productId) {
        log.info("🔗 [CIRCUIT BREAKER TEST] Checking stock for productId={}", productId);
        Map<String, Object> result = inventoryClient.checkStock(productId);
        return ResponseEntity.ok(result);
    }
}
