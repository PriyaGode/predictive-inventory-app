package com.shop.order.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SagaChoreographyProducer {

    private static final Logger log = LoggerFactory.getLogger(SagaChoreographyProducer.class);
    public static final String ORDER_SAGA_TOPIC = "order-saga-events";

    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;

    public SagaChoreographyProducer(KafkaTemplate<String, SagaEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(SagaEvent event) {
        event.setEventType("ORDER_CREATED");
        kafkaTemplate.send(ORDER_SAGA_TOPIC, event.getSagaId(), event);
        log.info("🟡 [SAGA-CHOREOGRAPHY] Published ORDER_CREATED → sagaId={} orderId={} productId={}",
                event.getSagaId(), event.getOrderId(), event.getProductId());
    }

    public void publishOrderCancelled(SagaEvent event, String reason) {
        event.setEventType("ORDER_CANCELLED");
        event.setFailureReason(reason);
        kafkaTemplate.send(ORDER_SAGA_TOPIC, event.getSagaId(), event);
        log.warn("🔴 [SAGA-CHOREOGRAPHY] Published ORDER_CANCELLED → sagaId={} reason={}", event.getSagaId(), reason);
    }

    public void publishOrderConfirmed(SagaEvent event) {
        event.setEventType("ORDER_CONFIRMED");
        kafkaTemplate.send(ORDER_SAGA_TOPIC, event.getSagaId(), event);
        log.info("🟢 [SAGA-CHOREOGRAPHY] Published ORDER_CONFIRMED → sagaId={} orderId={}", event.getSagaId(), event.getOrderId());
    }
}
