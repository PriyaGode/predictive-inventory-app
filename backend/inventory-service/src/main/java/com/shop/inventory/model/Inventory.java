package com.shop.inventory.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory", indexes = {
    // Task 2: Optimized indexes for fast inventory lookup queries
    @Index(name = "idx_product_id", columnList = "productId"),
    @Index(name = "idx_low_stock", columnList = "quantity, lowStockThreshold")
})
public class Inventory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long productId;

    private String productName;
    private Integer quantity;
    private Integer lowStockThreshold;
    private String status; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public Inventory() {}

    public Inventory(Long productId, String productName, Integer quantity, Integer lowStockThreshold) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.lowStockThreshold = lowStockThreshold;
        this.status = computeStatus(quantity, lowStockThreshold);
    }

    public static String computeStatus(int qty, int threshold) {
        if (qty == 0) return "OUT_OF_STOCK";
        if (qty <= threshold) return "LOW_STOCK";
        return "IN_STOCK";
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
