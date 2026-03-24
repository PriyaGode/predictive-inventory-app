package com.shop.inventory.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByProductId(Long productId);
    Optional<Supplier> findFirstByProductIdOrderByLeadTimeDaysAsc(Long productId);
}
