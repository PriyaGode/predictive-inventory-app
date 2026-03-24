package com.shop.inventory.supplier;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping("/suppliers")
    public ResponseEntity<Supplier> addSupplier(@RequestBody Supplier supplier) {
        return ResponseEntity.ok(supplierService.addSupplier(supplier));
    }

    @GetMapping("/suppliers")
    public List<Supplier> getAllSuppliers() {
        return supplierService.getAllSuppliers();
    }

    @GetMapping("/suppliers/product/{productId}")
    public List<Supplier> getSuppliersByProduct(@PathVariable Long productId) {
        return supplierService.getSuppliersByProduct(productId);
    }

    // Returns replenishment timelines for all products based on supplier lead times
    @GetMapping("/replenishment")
    public List<ReplenishmentTimeline> getReplenishmentTimelines() {
        return supplierService.getReplenishmentTimelines();
    }

    @GetMapping("/replenishment/{productId}")
    public ResponseEntity<ReplenishmentTimeline> getReplenishmentForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(supplierService.getReplenishmentForProduct(productId));
    }
}
