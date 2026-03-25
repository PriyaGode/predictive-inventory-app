package com.shop.inventory.service;

import com.shop.inventory.alert.AlertService;
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
    private final StockEventProducer  producer;
    private final AlertService        alertService;

    public InventoryService(InventoryRepository repository, StockEventProducer producer, AlertService alertService) {
        this.repository   = repository;
        this.producer     = producer;
        this.alertService = alertService;
    }

    @Cacheable(value = "inventory", key = "#productId")
    public Inventory getByProductId(Long productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for productId: " + productId));
    }

    @Cacheable(value = "lowStockItems")
    public List<Inventory> getLowStockItems() {
        return repository.findLowStockItems();
    }

    public List<Inventory> getAllInventory() {
        return repository.findAll();
    }

    /** Adjust physical quantity (restock / manual deduction). */
    @Transactional
    @CacheEvict(value = {"inventory", "lowStockItems"}, allEntries = true)
    public boolean updateStock(Long productId, Integer quantityChange, String reason) {
        log.info("🔄 [STOCK UPDATE] productId={} change={} reason={}", productId, quantityChange, reason);
        int rows = repository.updateStock(productId, quantityChange);
        if (rows == 0) {
            log.warn("⚠️ [STOCK UPDATE FAILED] productId={}", productId);
            return false;
        }
        publishEvent(productId, quantityChange, reason);
        alertService.evaluateProduct(productId);
        return true;
    }

    /** Reserve stock for a pending order — reduces availableStock, increases reservedStock. */
    @Transactional
    @CacheEvict(value = {"inventory", "lowStockItems"}, allEntries = true)
    public boolean reserveStock(Long productId, Integer qty) {
        log.info("🔒 [RESERVE] productId={} qty={}", productId, qty);
        int rows = repository.reserveStock(productId, qty);
        if (rows == 0) {
            log.warn("⚠️ [RESERVE FAILED] productId={} — insufficient available stock", productId);
            return false;
        }
        publishEvent(productId, -qty, "STOCK_RESERVED");
        alertService.evaluateProduct(productId);
        return true;
    }

    /** Release reserved stock (order cancelled or fulfilled). */
    @Transactional
    @CacheEvict(value = {"inventory", "lowStockItems"}, allEntries = true)
    public boolean releaseStock(Long productId, Integer qty) {
        log.info("🔓 [RELEASE] productId={} qty={}", productId, qty);
        int rows = repository.releaseStock(productId, qty);
        if (rows == 0) return false;
        publishEvent(productId, qty, "STOCK_RELEASED");
        return true;
    }

    /** Mark stock as incoming (supplier shipment dispatched). */
    @Transactional
    @CacheEvict(value = {"inventory", "lowStockItems"}, allEntries = true)
    public boolean addIncomingStock(Long productId, Integer qty) {
        log.info("🚚 [INCOMING] productId={} qty={}", productId, qty);
        return repository.addIncomingStock(productId, qty) > 0;
    }

    /** Receive incoming stock — moves it from incomingStock into physical quantity. */
    @Transactional
    @CacheEvict(value = {"inventory", "lowStockItems"}, allEntries = true)
    public boolean receiveIncomingStock(Long productId, Integer qty) {
        log.info("📥 [RECEIVE INCOMING] productId={} qty={}", productId, qty);
        int rows = repository.receiveIncomingStock(productId, qty);
        if (rows == 0) return false;
        publishEvent(productId, qty, "INCOMING_RECEIVED");
        return true;
    }

    @CacheEvict(value = {"inventory", "lowStockItems"}, allEntries = true)
    public Inventory addInventory(Inventory inventory) {
        inventory.setReservedStock(0);
        inventory.setIncomingStock(0);
        inventory.setStatus(Inventory.computeStatus(
                inventory.getQuantity(), 0, inventory.getLowStockThreshold()));
        inventory.setLastUpdated(java.time.LocalDateTime.now());
        return repository.save(inventory);
    }

    private void publishEvent(Long productId, Integer change, String reason) {
        repository.findByProductId(productId).ifPresent(inv ->
            producer.publishStockUpdate(new StockUpdateEvent(productId, inv.getProductName(), change, reason))
        );
    }
}
