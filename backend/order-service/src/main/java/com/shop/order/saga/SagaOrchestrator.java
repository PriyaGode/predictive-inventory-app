package com.shop.order.saga;

import com.shop.order.model.Order;
import com.shop.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * SAGA ORCHESTRATION:
 * The orchestrator drives the saga by sending commands to each service
 * and waiting for replies. It knows the full workflow.
 *
 * Flow:
 * 1. Order placed → Orchestrator sends RESERVE_STOCK command to inventory
 * 2. Inventory replies STOCK_RESERVED → Orchestrator sends PROCESS_PAYMENT command
 * 3. Payment replies PAYMENT_SUCCESS → Orchestrator confirms order
 * 4. Any failure → Orchestrator sends compensating commands (RELEASE_STOCK, REFUND)
 */
@Component
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);
    private static final String INVENTORY_COMMAND_TOPIC = "inventory-saga-commands";
    private static final String PAYMENT_COMMAND_TOPIC   = "payment-saga-commands";

    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;
    private final OrderRepository orderRepository;

    public SagaOrchestrator(KafkaTemplate<String, SagaEvent> kafkaTemplate, OrderRepository orderRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
    }

    // Step 1: Start saga — send RESERVE_STOCK command to inventory
    public void startOrderSaga(Order order, Long productId, Integer quantity) {
        SagaEvent event = new SagaEvent("RESERVE_STOCK", order.getId(), productId,
                null, quantity, order.getTotalAmount(), order.getCustomerEmail());

        kafkaTemplate.send(INVENTORY_COMMAND_TOPIC, event.getSagaId(), event);
        log.info("🎯 [SAGA-ORCHESTRATOR] Step 1 → Sent RESERVE_STOCK command to inventory | sagaId={} orderId={}",
                event.getSagaId(), order.getId());
    }

    // Step 2: Inventory replied STOCK_RESERVED → send PROCESS_PAYMENT command
    public void onStockReserved(SagaEvent event) {
        event.setEventType("PROCESS_PAYMENT");
        kafkaTemplate.send(PAYMENT_COMMAND_TOPIC, event.getSagaId(), event);
        log.info("🎯 [SAGA-ORCHESTRATOR] Step 2 → Sent PROCESS_PAYMENT command | sagaId={}", event.getSagaId());
    }

    // Step 3: Payment success → confirm order
    public void onPaymentSuccess(SagaEvent event) {
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus("CONFIRMED");
            orderRepository.save(order);
            log.info("✅ [SAGA-ORCHESTRATOR] Step 3 → Order CONFIRMED | sagaId={} orderId={}",
                    event.getSagaId(), order.getId());
        });
    }

    // Compensating Transaction: Stock failed → cancel order
    public void onStockFailed(SagaEvent event) {
        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            log.warn("🔴 [SAGA-ORCHESTRATOR] COMPENSATE → Order CANCELLED (stock failed) | sagaId={} reason={}",
                    event.getSagaId(), event.getFailureReason());
        });
    }

    // Compensating Transaction: Payment failed → release stock + cancel order
    public void onPaymentFailed(SagaEvent event) {
        // Send RELEASE_STOCK compensating command to inventory
        event.setEventType("RELEASE_STOCK");
        kafkaTemplate.send(INVENTORY_COMMAND_TOPIC, event.getSagaId(), event);
        log.warn("🔴 [SAGA-ORCHESTRATOR] COMPENSATE → Sent RELEASE_STOCK command | sagaId={}", event.getSagaId());

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            log.warn("🔴 [SAGA-ORCHESTRATOR] COMPENSATE → Order CANCELLED (payment failed) | sagaId={}", event.getSagaId());
        });
    }
}
