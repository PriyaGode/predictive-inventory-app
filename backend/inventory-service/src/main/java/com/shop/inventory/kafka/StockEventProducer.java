package com.shop.inventory.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class StockEventProducer {

    private static final Logger log = LoggerFactory.getLogger(StockEventProducer.class);
    private static final String TOPIC = "stock-updates";

    private final KafkaTemplate<String, StockUpdateEvent> kafkaTemplate;

    public StockEventProducer(KafkaTemplate<String, StockUpdateEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishStockUpdate(StockUpdateEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.getProductId()), event);
        log.info("📤 [KAFKA PRODUCER] Published stock event → productId={} change={} reason={}",
                event.getProductId(), event.getQuantityChange(), event.getReason());
    }
}
