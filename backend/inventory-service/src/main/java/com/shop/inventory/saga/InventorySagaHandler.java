package com.shop.inventory.saga;

import com.shop.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventorySagaHandler {

    private static final Logger log = LoggerFactory.getLogger(InventorySagaHandler.class);
    private static final String CHOREOGRAPHY_REPLY_TOPIC  = "inventory-saga-reply";
    private static final String ORCHESTRATION_REPLY_TOPIC = "orchestrator-saga-replies";

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;

    public InventorySagaHandler(InventoryRepository inventoryRepository,
                                @Qualifier("sagaKafkaTemplate") KafkaTemplate<String, SagaEvent> kafkaTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // CHOREOGRAPHY: listens to ORDER_CREATED events, reserves stock, replies to order-service
    @Transactional
    @KafkaListener(topics = "order-saga-events", groupId = "inventory-saga-group",
                   containerFactory = "sagaKafkaListenerContainerFactory")
    public void onOrderSagaEvent(SagaEvent event) {
        if (!"ORDER_CREATED".equals(event.getEventType())) return;

        log.info("📥 [SAGA-CHOREOGRAPHY] Received ORDER_CREATED → sagaId={} productId={} qty={}",
                event.getSagaId(), event.getProductId(), event.getQuantity());

        boolean reserved = tryReserveStock(event.getProductId(), event.getQuantity());

        if (reserved) {
            event.setEventType("STOCK_RESERVED");
            kafkaTemplate.send(CHOREOGRAPHY_REPLY_TOPIC, event.getSagaId(), event);
            log.info("🟢 [SAGA-CHOREOGRAPHY] STOCK_RESERVED → sagaId={}", event.getSagaId());
        } else {
            event.setEventType("STOCK_FAILED");
            event.setFailureReason("Insufficient stock for productId=" + event.getProductId());
            kafkaTemplate.send(CHOREOGRAPHY_REPLY_TOPIC, event.getSagaId(), event);
            log.warn("🔴 [SAGA-CHOREOGRAPHY] STOCK_FAILED → sagaId={}", event.getSagaId());
        }
    }

    // ORCHESTRATION: listens to RESERVE_STOCK / RELEASE_STOCK commands from orchestrator
    @Transactional
    @KafkaListener(topics = "inventory-saga-commands", groupId = "inventory-orchestrator-group",
                   containerFactory = "sagaKafkaListenerContainerFactory")
    public void onInventoryCommand(SagaEvent event) {
        log.info("📥 [SAGA-ORCHESTRATOR] Command received → eventType={} sagaId={}",
                event.getEventType(), event.getSagaId());

        if ("RESERVE_STOCK".equals(event.getEventType())) {
            boolean reserved = tryReserveStock(event.getProductId(), event.getQuantity());
            event.setEventType(reserved ? "STOCK_RESERVED" : "STOCK_FAILED");
            if (!reserved) event.setFailureReason("Insufficient stock for productId=" + event.getProductId());
            kafkaTemplate.send(ORCHESTRATION_REPLY_TOPIC, event.getSagaId(), event);
            log.info("📤 [SAGA-ORCHESTRATOR] Replied {} → sagaId={}", event.getEventType(), event.getSagaId());

        } else if ("RELEASE_STOCK".equals(event.getEventType())) {
            // Compensating transaction: restore stock
            inventoryRepository.updateStock(event.getProductId(), event.getQuantity());
            log.info("↩️ [SAGA-ORCHESTRATOR] Stock RELEASED (compensation) → sagaId={} productId={}",
                    event.getSagaId(), event.getProductId());
        }
    }

    private boolean tryReserveStock(Long productId, Integer quantity) {
        int rows = inventoryRepository.updateStock(productId, -quantity);
        return rows > 0;
    }
}
