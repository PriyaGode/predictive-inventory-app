package com.shop.inventory.supplier;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String supplierName;
    private Long productId;
    private Integer leadTimeDays;       // days from order to delivery
    private Integer reorderQuantity;    // how much to reorder
    private String contactEmail;
    private Integer deliveryDelayDays = 0;   // current reported delay on top of lead time
    private Boolean active = true;            // false = supplier unavailable / slow
    private LocalDateTime createdAt = LocalDateTime.now();

    public Supplier() {}

    public Supplier(String supplierName, Long productId, Integer leadTimeDays,
                    Integer reorderQuantity, String contactEmail) {
        this.supplierName = supplierName;
        this.productId = productId;
        this.leadTimeDays = leadTimeDays;
        this.reorderQuantity = reorderQuantity;
        this.contactEmail = contactEmail;
        this.deliveryDelayDays = 0;
        this.active = true;
    }

    public Long getId() { return id; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public Integer getReorderQuantity() { return reorderQuantity; }
    public void setReorderQuantity(Integer reorderQuantity) { this.reorderQuantity = reorderQuantity; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public Integer getDeliveryDelayDays() { return deliveryDelayDays != null ? deliveryDelayDays : 0; }
    public void setDeliveryDelayDays(Integer deliveryDelayDays) { this.deliveryDelayDays = deliveryDelayDays; }
    public Boolean getActive() { return active != null ? active : true; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
