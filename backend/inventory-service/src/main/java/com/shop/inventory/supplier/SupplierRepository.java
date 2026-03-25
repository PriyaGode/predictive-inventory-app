package com.shop.inventory.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByProductId(Long productId);
    Optional<Supplier> findFirstByProductIdOrderByLeadTimeDaysAsc(Long productId);
    List<Supplier> findByActiveTrue();
    List<Supplier> findByDeliveryDelayDaysGreaterThan(int threshold);

    @Query("SELECT s FROM Supplier s WHERE s.productId = :productId AND s.active = true ORDER BY s.leadTimeDays ASC")
    List<Supplier> findActiveByProductId(Long productId);
}
