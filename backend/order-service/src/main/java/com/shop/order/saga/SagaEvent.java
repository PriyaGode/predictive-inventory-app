package com.shop.order.saga;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class SagaEvent implements Serializable {

    private String sagaId;
    private String eventType;   // ORDER_CREATED, STOCK_RESERVED, STOCK_FAILED, ORDER_CONFIRMED, ORDER_CANCELLED
    private Long orderId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double totalAmount;
    private String customerEmail;
    private String failureReason;
    private LocalDateTime timestamp = LocalDateTime.now();

    public SagaEvent() {}

    public SagaEvent(String eventType, Long orderId, Long productId, String productName,
                     Integer quantity, Double totalAmount, String customerEmail) {
        this.sagaId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.customerEmail = customerEmail;
    }

    public String getSagaId() { return sagaId; }
    public void setSagaId(String sagaId) { this.sagaId = sagaId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
