package com.shop.inventory.controller;

import com.shop.inventory.kafka.StockUpdateEvent;
import com.shop.inventory.model.Inventory;
import com.shop.inventory.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Inventory> getAll() {
        return service.getAllInventory();
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Inventory> getByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(service.getByProductId(productId));
    }

    // Task 5: Low-stock alerts endpoint for React dashboard
    @GetMapping("/low-stock")
    public List<Inventory> getLowStockAlerts() {
        return service.getLowStockItems();
    }

    // Task 1: Manual stock update endpoint (also triggers Kafka event)
    @PostMapping("/update-stock")
    public ResponseEntity<String> updateStock(@RequestBody StockUpdateEvent event) {
        boolean success = service.updateStock(event.getProductId(), event.getQuantityChange(), event.getReason());
        if (success) return ResponseEntity.ok("Stock updated successfully");
        return ResponseEntity.badRequest().body("Stock update failed — insufficient stock");
    }

    @PostMapping
    public ResponseEntity<Inventory> addInventory(@RequestBody Inventory inventory) {
        return ResponseEntity.ok(service.addInventory(inventory));
    }
}
