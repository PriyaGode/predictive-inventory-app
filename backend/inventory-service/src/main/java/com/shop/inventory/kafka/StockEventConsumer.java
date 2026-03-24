package com.shop.inventory.kafka;

import com.shop.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class StockEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockEventConsumer.class);

    private final InventoryService inventoryService;

    public StockEventConsumer(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // Retry up to 3 times with exponential backoff (1s → 2s → 4s), then send to DLT
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "order-events", groupId = "inventory-group")
    public void onOrderEvent(StockUpdateEvent event,
                             @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                             @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("📥 [KAFKA] topic={} offset={} | productId={} change={} reason={}",
                topic, offset, event.getProductId(), event.getQuantityChange(), event.getReason());

        if (event.getProductId() == null || event.getQuantityChange() == null) {
            log.error("❌ [KAFKA] Invalid event — missing productId or quantityChange. Skipping. offset={}", offset);
            return;
        }

        boolean updated = inventoryService.updateStock(
                event.getProductId(), event.getQuantityChange(), event.getReason());

        if (updated) {
            log.info("✅ [KAFKA] Stock updated | productId={} change={}", event.getProductId(), event.getQuantityChange());
        } else {
            log.warn("⚠️ [KAFKA] Stock update FAILED | productId={} — insufficient stock or not found", event.getProductId());
            // Throw to trigger retry — inventory may be temporarily locked
            throw new RuntimeException("Stock update failed for productId=" + event.getProductId());
        }
    }

    // Dead Letter Topic handler — called after all retries exhausted
    @KafkaListener(topics = "order-events-dlt", groupId = "inventory-dlt-group")
    public void onOrderEventDlt(StockUpdateEvent event,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                @Header(KafkaHeaders.OFFSET) long offset) {
        log.error("🚨 [KAFKA-DLT] Message moved to Dead Letter Topic | topic={} offset={} productId={} reason={}",
                topic, offset, event.getProductId(), event.getReason());
        // TODO: alert ops team / store in failed_events table for manual review
    }

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "stock-updates", groupId = "inventory-group")
    public void onStockUpdate(StockUpdateEvent event,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                              @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("📥 [KAFKA] topic={} offset={} | productId={} reason={}",
                topic, offset, event.getProductId(), event.getReason());
    }
}
