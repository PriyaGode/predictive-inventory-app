package com.shop.inventory.service;

import com.shop.inventory.kafka.StockEventProducer;
import com.shop.inventory.kafka.StockUpdateEvent;
import com.shop.inventory.model.Inventory;
import com.shop.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository repository;
    private final StockEventProducer producer;

    public InventoryService(InventoryRepository repository, StockEventProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    // Task 3: Redis cache for frequently accessed inventory data
    @Cacheable(value = "inventory", key = "#productId")
    public Inventory getByProductId(Long productId) {
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║  [DATABASE] Fetching inventory productId={}   ║", productId);
        log.info("╚══════════════════════════════════════════════╝");
        return repository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for productId: " + productId));
    }

    // Task 2: Optimized query using index idx_low_stock
    @Cacheable(value = "lowStockItems")
    public List<Inventory> getLowStockItems() {
        log.info("╔══════════════════════════════════════════════╗");
        log.info("║  [DATABASE] Fetching LOW STOCK items (optimized query) ║");
        log.info("╚══════════════════════════════════════════════╝");
        return repository.findLowStockItems();
    }

    public List<Inventory> getAllInventory() {
        return repository.findAll();
    }

    // Task 4: Fixed bug - stock now correctly updates using atomic DB operation
    @Transactional
    @CacheEvict(value = {"inventory", "lowStockItems"}, allEntries = true)
    public boolean updateStock(Long productId, Integer quantityChange, String reason) {
        log.info("🔄 [STOCK UPDATE] productId={} change={} reason={}", productId, quantityChange, reason);

        // Task 4 Fix: Use single atomic UPDATE query instead of SELECT + UPDATE
        // This prevents race conditions and ensures stock never goes negative
        int rowsUpdated = repository.updateStock(productId, quantityChange);

        if (rowsUpdated == 0) {
            log.warn("⚠️ [STOCK UPDATE FAILED] productId={} — insufficient stock or not found", productId);
            return false;
        }

        // Task 1: Publish real-time Kafka event after stock update
        Inventory updated = repository.findByProductId(productId).orElse(null);
        if (updated != null) {
            producer.publishStockUpdate(new StockUpdateEvent(
                    productId, updated.getProductName(), quantityChange, reason));
            log.info("✅ [STOCK UPDATE SUCCESS] productId={} newQty={} status={}",
                    productId, updated.getQuantity(), updated.getStatus());
        }

        return true;
    }

    @CacheEvict(value = {"inventory", "lowStockItems"}, allEntries = true)
    public Inventory addInventory(Inventory inventory) {
        inventory.setStatus(Inventory.computeStatus(inventory.getQuantity(), inventory.getLowStockThreshold()));
        inventory.setLastUpdated(java.time.LocalDateTime.now());
        return repository.save(inventory);
    }
}
