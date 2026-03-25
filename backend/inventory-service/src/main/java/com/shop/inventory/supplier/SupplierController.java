package com.shop.inventory.supplier;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    // ── Supplier CRUD ─────────────────────────────────────────────────────────

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

    // ── Lead Times ────────────────────────────────────────────────────────────

    /** GET /api/inventory/suppliers/lead-times — all supplier lead times */
    @GetMapping("/suppliers/lead-times")
    public List<LeadTimeResponse> getAllLeadTimes() {
        return supplierService.getAllLeadTimes();
    }

    /** GET /api/inventory/suppliers/lead-times/{productId} — lead times for one product */
    @GetMapping("/suppliers/lead-times/{productId}")
    public ResponseEntity<List<LeadTimeResponse>> getLeadTimesForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(supplierService.getLeadTimesForProduct(productId));
    }

    // ── Stock Availability ────────────────────────────────────────────────────

    /** GET /api/inventory/availability — stock availability for all products */
    @GetMapping("/availability")
    public List<StockAvailabilityResponse> getAllStockAvailability() {
        return supplierService.getAllStockAvailability();
    }

    /** GET /api/inventory/availability/{productId} — stock availability for one product */
    @GetMapping("/availability/{productId}")
    public ResponseEntity<StockAvailabilityResponse> getStockAvailability(@PathVariable Long productId) {
        return ResponseEntity.ok(supplierService.getStockAvailability(productId));
    }

    // ── Delivery Delays ───────────────────────────────────────────────────────

    /** GET /api/inventory/suppliers/delays — all suppliers with active delays */
    @GetMapping("/suppliers/delays")
    public List<DeliveryDelayResponse> getActiveDelays() {
        return supplierService.getActiveDeliveryDelays();
    }

    /**
     * POST /api/inventory/suppliers/{supplierId}/delay
     * Body: { "delayDays": 5, "reason": "Port congestion" }
     */
    @PostMapping("/suppliers/{supplierId}/delay")
    public ResponseEntity<DeliveryDelayResponse> reportDelay(
            @PathVariable Long supplierId,
            @RequestBody Map<String, Object> body) {
        int delayDays = Integer.parseInt(body.getOrDefault("delayDays", "0").toString());
        String reason = body.getOrDefault("reason", "Not specified").toString();
        return ResponseEntity.ok(supplierService.reportDelay(supplierId, delayDays, reason));
    }

    /** DELETE /api/inventory/suppliers/{supplierId}/delay — clear a delay */
    @DeleteMapping("/suppliers/{supplierId}/delay")
    public ResponseEntity<DeliveryDelayResponse> clearDelay(@PathVariable Long supplierId) {
        return ResponseEntity.ok(supplierService.clearDelay(supplierId));
    }

    // ── Replenishment Timelines (existing) ───────────────────────────────────

    @GetMapping("/replenishment")
    public List<ReplenishmentTimeline> getReplenishmentTimelines() {
        return supplierService.getReplenishmentTimelines();
    }

    @GetMapping("/replenishment/{productId}")
    public ResponseEntity<ReplenishmentTimeline> getReplenishmentForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(supplierService.getReplenishmentForProduct(productId));
    }
}
