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

    @PostMapping("/update-stock")
    public ResponseEntity<String> updateStock(@RequestBody StockUpdateEvent event) {
        boolean success = service.updateStock(event.getProductId(), event.getQuantityChange(), event.getReason());
        if (success) return ResponseEntity.ok("Stock updated successfully");
        return ResponseEntity.badRequest().body("Stock update failed — insufficient stock");
    }

    /** Reserve stock for a pending order */
    @PostMapping("/product/{productId}/reserve")
    public ResponseEntity<String> reserveStock(@PathVariable Long productId, @RequestParam int qty) {
        boolean ok = service.reserveStock(productId, qty);
        return ok ? ResponseEntity.ok("Reserved " + qty + " units for productId=" + productId)
                  : ResponseEntity.badRequest().body("Reserve failed — insufficient available stock");
    }

    /** Release reserved stock (order cancelled or fulfilled) */
    @PostMapping("/product/{productId}/release")
    public ResponseEntity<String> releaseStock(@PathVariable Long productId, @RequestParam int qty) {
        boolean ok = service.releaseStock(productId, qty);
        return ok ? ResponseEntity.ok("Released " + qty + " units for productId=" + productId)
                  : ResponseEntity.badRequest().body("Release failed");
    }

    /** Mark stock as incoming (supplier shipment dispatched) */
    @PostMapping("/product/{productId}/incoming")
    public ResponseEntity<String> addIncoming(@PathVariable Long productId, @RequestParam int qty) {
        boolean ok = service.addIncomingStock(productId, qty);
        return ok ? ResponseEntity.ok("Marked " + qty + " units as incoming for productId=" + productId)
                  : ResponseEntity.badRequest().body("Product not found");
    }

    /** Receive incoming stock — moves it into physical quantity */
    @PostMapping("/product/{productId}/receive")
    public ResponseEntity<String> receiveIncoming(@PathVariable Long productId, @RequestParam int qty) {
        boolean ok = service.receiveIncomingStock(productId, qty);
        return ok ? ResponseEntity.ok("Received " + qty + " units into stock for productId=" + productId)
                  : ResponseEntity.badRequest().body("Receive failed");
    }

    @PostMapping
    public ResponseEntity<Inventory> addInventory(@RequestBody Inventory inventory) {
        return ResponseEntity.ok(service.addInventory(inventory));
    }
}
