package com.shop.inventory.kafka;

import java.io.Serializable;
import java.time.LocalDateTime;

public class StockUpdateEvent implements Serializable {

    private Long productId;
    private String productName;
    private Integer quantityChange; // negative = deduct, positive = restock
    private String reason;          // ORDER_PLACED, ORDER_CANCELLED, RESTOCK
    private LocalDateTime timestamp = LocalDateTime.now();

    public StockUpdateEvent() {}

    public StockUpdateEvent(Long productId, String productName, Integer quantityChange, String reason) {
        this.productId = productId;
        this.productName = productName;
        this.quantityChange = quantityChange;
        this.reason = reason;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
