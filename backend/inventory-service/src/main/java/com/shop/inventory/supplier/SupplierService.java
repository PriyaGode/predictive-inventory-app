package com.shop.inventory.supplier;

import com.shop.inventory.exception.SupplierNotFoundException;
import com.shop.inventory.model.Inventory;
import com.shop.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierService.class);
    private static final int DELAY_THRESHOLD = 0;

    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;

    public SupplierService(SupplierRepository supplierRepository, InventoryRepository inventoryRepository) {
        this.supplierRepository = supplierRepository;
        this.inventoryRepository = inventoryRepository;
    }

    // ── Basic CRUD ────────────────────────────────────────────────────────────

    public Supplier addSupplier(Supplier supplier) {
        validateSupplier(supplier);
        log.info("➕ [SUPPLIER] Adding supplier={} for productId={}", supplier.getSupplierName(), supplier.getProductId());
        return supplierRepository.save(supplier);
    }

    public List<Supplier> getSuppliersByProduct(Long productId) {
        return supplierRepository.findByProductId(productId);
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    // ── Lead Times ────────────────────────────────────────────────────────────

    /** Lead times for all suppliers across all products */
    public List<LeadTimeResponse> getAllLeadTimes() {
        List<Supplier> suppliers = supplierRepository.findAll();
        if (suppliers.isEmpty()) {
            log.warn("⚠️ [LEAD TIME] No suppliers found in system");
        }
        return suppliers.stream()
                .map(s -> {
                    String productName = inventoryRepository.findByProductId(s.getProductId())
                            .map(Inventory::getProductName).orElse("Unknown Product");
                    return LeadTimeResponse.from(s, productName);
                })
                .collect(Collectors.toList());
    }

    /** Lead times for all suppliers of a specific product */
    public List<LeadTimeResponse> getLeadTimesForProduct(Long productId) {
        List<Supplier> suppliers = supplierRepository.findByProductId(productId);
        if (suppliers.isEmpty()) {
            throw new SupplierNotFoundException("No suppliers found for productId: " + productId);
        }
        String productName = inventoryRepository.findByProductId(productId)
                .map(Inventory::getProductName).orElse("Unknown Product");
        log.info("📋 [LEAD TIME] Fetching lead times for productId={} — {} supplier(s)", productId, suppliers.size());
        return suppliers.stream()
                .map(s -> LeadTimeResponse.from(s, productName))
                .collect(Collectors.toList());
    }

    // ── Stock Availability ────────────────────────────────────────────────────

    /** Stock availability for all products, enriched with fastest active supplier */
    public List<StockAvailabilityResponse> getAllStockAvailability() {
        return inventoryRepository.findAll().stream()
                .map(inv -> {
                    Supplier supplier = supplierRepository
                            .findFirstByProductIdOrderByLeadTimeDaysAsc(inv.getProductId())
                            .orElse(null);
                    if (supplier == null) {
                        log.warn("⚠️ [AVAILABILITY] No supplier for productId={}", inv.getProductId());
                    }
                    return StockAvailabilityResponse.from(inv, supplier);
                })
                .collect(Collectors.toList());
    }

    /** Stock availability for a single product */
    public StockAvailabilityResponse getStockAvailability(Long productId) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new SupplierNotFoundException("No inventory found for productId: " + productId));
        Supplier supplier = supplierRepository
                .findFirstByProductIdOrderByLeadTimeDaysAsc(productId)
                .orElse(null);
        if (supplier == null) {
            log.warn("⚠️ [AVAILABILITY] No supplier assigned for productId={} — returning partial data", productId);
        }
        return StockAvailabilityResponse.from(inv, supplier);
    }

    // ── Delivery Delays ───────────────────────────────────────────────────────

    /** All suppliers currently reporting a delivery delay */
    public List<DeliveryDelayResponse> getActiveDeliveryDelays() {
        List<Supplier> delayed = supplierRepository.findByDeliveryDelayDaysGreaterThan(DELAY_THRESHOLD);
        if (delayed.isEmpty()) {
            log.info("✅ [DELAYS] No active delivery delays");
        } else {
            log.warn("⚠️ [DELAYS] {} supplier(s) reporting delays", delayed.size());
        }
        return delayed.stream()
                .map(s -> DeliveryDelayResponse.from(s, null))
                .collect(Collectors.toList());
    }

    /** Report or update a delivery delay for a specific supplier */
    public DeliveryDelayResponse reportDelay(Long supplierId, int delayDays, String reason) {
        if (delayDays < 0) {
            throw new IllegalArgumentException("delayDays cannot be negative: " + delayDays);
        }
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier not found: id=" + supplierId));

        supplier.setDeliveryDelayDays(delayDays);
        supplier.setActive(delayDays == 0);   // mark inactive only when delay is reported as ongoing
        supplierRepository.save(supplier);

        log.warn("🚨 [DELAY REPORTED] supplierId={} supplier={} delayDays={} reason={}",
                supplierId, supplier.getSupplierName(), delayDays, reason);
        return DeliveryDelayResponse.from(supplier, reason);
    }

    /** Clear a delivery delay (supplier back to normal) */
    public DeliveryDelayResponse clearDelay(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier not found: id=" + supplierId));
        supplier.setDeliveryDelayDays(0);
        supplier.setActive(true);
        supplierRepository.save(supplier);
        log.info("✅ [DELAY CLEARED] supplierId={} supplier={}", supplierId, supplier.getSupplierName());
        return DeliveryDelayResponse.from(supplier, "Delay cleared");
    }

    // ── Replenishment Timelines (existing) ───────────────────────────────────

    public List<ReplenishmentTimeline> getReplenishmentTimelines() {
        return inventoryRepository.findAll().stream()
                .map(inv -> {
                    ReplenishmentTimeline timeline = new ReplenishmentTimeline();
                    timeline.setProductId(inv.getProductId());
                    timeline.setProductName(inv.getProductName());
                    timeline.setCurrentStock(inv.getQuantity());
                    timeline.setLowStockThreshold(inv.getLowStockThreshold());
                    timeline.setStockStatus(inv.getStatus());
                    timeline.setReplenishmentNeeded(!inv.getStatus().equals("IN_STOCK"));

                    supplierRepository.findFirstByProductIdOrderByLeadTimeDaysAsc(inv.getProductId())
                            .ifPresentOrElse(supplier -> {
                                int effective = supplier.getLeadTimeDays() + supplier.getDeliveryDelayDays();
                                timeline.setSupplierName(supplier.getSupplierName());
                                timeline.setLeadTimeDays(effective);
                                timeline.setReorderQuantity(supplier.getReorderQuantity());
                                timeline.setReplenishmentDate(LocalDate.now().plusDays(effective));
                            }, () -> {
                                timeline.setSupplierName("No supplier assigned");
                                timeline.setLeadTimeDays(0);
                                timeline.setReorderQuantity(0);
                                timeline.setReplenishmentDate(null);
                            });

                    log.info("📦 [REPLENISHMENT] productId={} status={} effectiveLeadTime={}d",
                            inv.getProductId(), inv.getStatus(), timeline.getLeadTimeDays());
                    return timeline;
                })
                .collect(Collectors.toList());
    }

    public ReplenishmentTimeline getReplenishmentForProduct(Long productId) {
        return getReplenishmentTimelines().stream()
                .filter(t -> t.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new SupplierNotFoundException("No inventory found for productId: " + productId));
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateSupplier(Supplier supplier) {
        if (supplier.getSupplierName() == null || supplier.getSupplierName().isBlank()) {
            throw new IllegalArgumentException("supplierName is required");
        }
        if (supplier.getProductId() == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (supplier.getLeadTimeDays() == null || supplier.getLeadTimeDays() < 0) {
            throw new IllegalArgumentException("leadTimeDays must be a non-negative integer");
        }
        if (supplier.getReorderQuantity() == null || supplier.getReorderQuantity() <= 0) {
            throw new IllegalArgumentException("reorderQuantity must be a positive integer");
        }
    }
}
