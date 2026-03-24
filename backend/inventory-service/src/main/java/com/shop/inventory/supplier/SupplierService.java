package com.shop.inventory.supplier;

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

    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;

    public SupplierService(SupplierRepository supplierRepository, InventoryRepository inventoryRepository) {
        this.supplierRepository = supplierRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public Supplier addSupplier(Supplier supplier) {
        log.info("➕ [SUPPLIER] Adding supplier={} for productId={}", supplier.getSupplierName(), supplier.getProductId());
        return supplierRepository.save(supplier);
    }

    public List<Supplier> getSuppliersByProduct(Long productId) {
        return supplierRepository.findByProductId(productId);
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    // Calculate replenishment timeline for all low/out-of-stock items
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

                    // Find fastest supplier for this product
                    supplierRepository.findFirstByProductIdOrderByLeadTimeDaysAsc(inv.getProductId())
                            .ifPresentOrElse(supplier -> {
                                timeline.setSupplierName(supplier.getSupplierName());
                                timeline.setLeadTimeDays(supplier.getLeadTimeDays());
                                timeline.setReorderQuantity(supplier.getReorderQuantity());
                                timeline.setReplenishmentDate(LocalDate.now().plusDays(supplier.getLeadTimeDays()));
                            }, () -> {
                                timeline.setSupplierName("No supplier assigned");
                                timeline.setLeadTimeDays(0);
                                timeline.setReorderQuantity(0);
                                timeline.setReplenishmentDate(null);
                            });

                    log.info("📦 [REPLENISHMENT] productId={} status={} leadTime={}d replenishDate={}",
                            inv.getProductId(), inv.getStatus(),
                            timeline.getLeadTimeDays(), timeline.getReplenishmentDate());
                    return timeline;
                })
                .collect(Collectors.toList());
    }

    // Calculate replenishment timeline for a single product
    public ReplenishmentTimeline getReplenishmentForProduct(Long productId) {
        return getReplenishmentTimelines().stream()
                .filter(t -> t.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No inventory found for productId: " + productId));
    }
}
