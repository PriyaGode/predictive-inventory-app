package com.shop.inventory.supplier;

import java.time.LocalDate;

public class ReplenishmentTimeline {

    private Long productId;
    private String productName;
    private Integer currentStock;
    private Integer lowStockThreshold;
    private String stockStatus;
    private String supplierName;
    private Integer leadTimeDays;
    private Integer reorderQuantity;
    private LocalDate replenishmentDate;   // today + leadTimeDays
    private boolean replenishmentNeeded;

    public ReplenishmentTimeline() {}

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getCurrentStock() { return currentStock; }
    public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public String getStockStatus() { return stockStatus; }
    public void setStockStatus(String stockStatus) { this.stockStatus = stockStatus; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public Integer getReorderQuantity() { return reorderQuantity; }
    public void setReorderQuantity(Integer reorderQuantity) { this.reorderQuantity = reorderQuantity; }
    public LocalDate getReplenishmentDate() { return replenishmentDate; }
    public void setReplenishmentDate(LocalDate replenishmentDate) { this.replenishmentDate = replenishmentDate; }
    public boolean isReplenishmentNeeded() { return replenishmentNeeded; }
    public void setReplenishmentNeeded(boolean replenishmentNeeded) { this.replenishmentNeeded = replenishmentNeeded; }
}
