package com.shop.inventory.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_product_id", columnList = "productId"),
    @Index(name = "idx_low_stock",  columnList = "quantity, lowStockThreshold")
})
public class Inventory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long productId;

    private String  productName;
    private Integer quantity;           // total physical stock on shelf
    private Integer reservedStock  = 0; // held for pending orders (not yet shipped)
    private Integer incomingStock  = 0; // en-route from supplier (not yet received)
    private Integer lowStockThreshold;
    private String  status;             // based on availableStock
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public Inventory() {}

    public Inventory(Long productId, String productName, Integer quantity, Integer lowStockThreshold) {
        this.productId         = productId;
        this.productName       = productName;
        this.quantity          = quantity;
        this.reservedStock     = 0;
        this.incomingStock     = 0;
        this.lowStockThreshold = lowStockThreshold;
        this.status            = computeStatus(quantity, 0, lowStockThreshold);
    }

    /** availableStock = quantity - reservedStock (what customers can actually buy) */
    @Transient
    public int getAvailableStock() {
        int reserved = reservedStock != null ? reservedStock : 0;
        int qty      = quantity      != null ? quantity      : 0;
        return Math.max(0, qty - reserved);
    }

    public static String computeStatus(int qty, int reserved, int threshold) {
        int available = Math.max(0, qty - reserved);
        if (available == 0)         return "OUT_OF_STOCK";
        if (available <= threshold) return "LOW_STOCK";
        return "IN_STOCK";
    }

    public Long getId()                          { return id; }
    public Long getProductId()                   { return productId; }
    public void setProductId(Long v)             { this.productId = v; }
    public String getProductName()               { return productName; }
    public void setProductName(String v)         { this.productName = v; }
    public Integer getQuantity()                 { return quantity; }
    public void setQuantity(Integer v)           { this.quantity = v; }
    public Integer getReservedStock()            { return reservedStock != null ? reservedStock : 0; }
    public void setReservedStock(Integer v)      { this.reservedStock = v; }
    public Integer getIncomingStock()            { return incomingStock != null ? incomingStock : 0; }
    public void setIncomingStock(Integer v)      { this.incomingStock = v; }
    public Integer getLowStockThreshold()        { return lowStockThreshold; }
    public void setLowStockThreshold(Integer v)  { this.lowStockThreshold = v; }
    public String getStatus()                    { return status; }
    public void setStatus(String v)              { this.status = v; }
    public LocalDateTime getLastUpdated()        { return lastUpdated; }
    public void setLastUpdated(LocalDateTime v)  { this.lastUpdated = v; }
}
