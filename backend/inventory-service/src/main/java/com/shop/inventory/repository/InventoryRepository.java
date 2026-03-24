package com.shop.inventory.repository;

import com.shop.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    // Task 2: Optimized query - uses idx_low_stock index, avoids full table scan
    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.lowStockThreshold ORDER BY i.quantity ASC")
    List<Inventory> findLowStockItems();

    // Task 2: Optimized bulk stock deduction - single UPDATE instead of SELECT + UPDATE
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :change, " +
           "i.status = CASE " +
           "  WHEN (i.quantity + :change) = 0 THEN 'OUT_OF_STOCK' " +
           "  WHEN (i.quantity + :change) <= i.lowStockThreshold THEN 'LOW_STOCK' " +
           "  ELSE 'IN_STOCK' END, " +
           "i.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE i.productId = :productId AND (i.quantity + :change) >= 0")
    int updateStock(@Param("productId") Long productId, @Param("change") Integer change);
}
