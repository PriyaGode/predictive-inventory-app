package com.shop.order.saga;

import com.shop.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SagaChoreographyConsumer {

    private static final Logger log = LoggerFactory.getLogger(SagaChoreographyConsumer.class);

    private final OrderRepository orderRepository;
    private final SagaChoreographyProducer producer;

    public SagaChoreographyConsumer(OrderRepository orderRepository, SagaChoreographyProducer producer) {
        this.orderRepository = orderRepository;
        this.producer = producer;
    }

    // Listens to inventory replies: STOCK_RESERVED or STOCK_FAILED
    @KafkaListener(topics = "inventory-saga-reply", groupId = "order-saga-group")
    public void onInventoryReply(SagaEvent event) {
        log.info("📥 [SAGA-CHOREOGRAPHY] Received inventory reply → eventType={} sagaId={} orderId={}",
                event.getEventType(), event.getSagaId(), event.getOrderId());

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {
            if ("STOCK_RESERVED".equals(event.getEventType())) {
                // Stock reserved → confirm order
                order.setStatus("CONFIRMED");
                orderRepository.save(order);
                producer.publishOrderConfirmed(event);
                log.info("🟢 [SAGA-CHOREOGRAPHY] Order CONFIRMED → orderId={}", order.getId());

            } else if ("STOCK_FAILED".equals(event.getEventType())) {
                // Stock failed → compensate → cancel order
                order.setStatus("CANCELLED");
                orderRepository.save(order);
                producer.publishOrderCancelled(event, event.getFailureReason());
                log.warn("🔴 [SAGA-CHOREOGRAPHY] Order CANCELLED (compensating tx) → orderId={} reason={}",
                        order.getId(), event.getFailureReason());
            }
        });
    }
}
