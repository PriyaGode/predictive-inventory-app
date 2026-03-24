package com.shop.order.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to all saga reply topics and delegates to the orchestrator.
 * This is the "brain" that drives the workflow forward or triggers compensation.
 */
@Component
public class SagaOrchestratorReplyConsumer {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestratorReplyConsumer.class);

    private final SagaOrchestrator orchestrator;

    public SagaOrchestratorReplyConsumer(SagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(topics = "orchestrator-saga-replies", groupId = "orchestrator-group")
    public void onSagaReply(SagaEvent event) {
        log.info("📥 [SAGA-ORCHESTRATOR] Reply received → eventType={} sagaId={}",
                event.getEventType(), event.getSagaId());

        switch (event.getEventType()) {
            case "STOCK_RESERVED"    -> orchestrator.onStockReserved(event);
            case "STOCK_FAILED"      -> orchestrator.onStockFailed(event);
            case "PAYMENT_SUCCESS"   -> orchestrator.onPaymentSuccess(event);
            case "PAYMENT_FAILED"    -> orchestrator.onPaymentFailed(event);
            default -> log.warn("⚠️ [SAGA-ORCHESTRATOR] Unknown event type: {}", event.getEventType());
        }
    }
}
