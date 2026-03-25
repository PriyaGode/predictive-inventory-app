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

    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedStock) <= i.lowStockThreshold ORDER BY (i.quantity - i.reservedStock) ASC")
    List<Inventory> findLowStockItems();

    /** Adjust physical quantity (restock / order deduction). Status based on available = qty - reserved. */
    @Modifying
    @Query("UPDATE Inventory i SET " +
           "i.quantity = i.quantity + :change, " +
           "i.status = CASE " +
           "  WHEN (i.quantity + :change - i.reservedStock) <= 0 THEN 'OUT_OF_STOCK' " +
           "  WHEN (i.quantity + :change - i.reservedStock) <= i.lowStockThreshold THEN 'LOW_STOCK' " +
           "  ELSE 'IN_STOCK' END, " +
           "i.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE i.productId = :productId AND (i.quantity + :change) >= 0")
    int updateStock(@Param("productId") Long productId, @Param("change") Integer change);

    /** Reserve stock for a pending order — increases reservedStock, reduces available. */
    @Modifying
    @Query("UPDATE Inventory i SET " +
           "i.reservedStock = i.reservedStock + :qty, " +
           "i.status = CASE " +
           "  WHEN (i.quantity - i.reservedStock - :qty) <= 0 THEN 'OUT_OF_STOCK' " +
           "  WHEN (i.quantity - i.reservedStock - :qty) <= i.lowStockThreshold THEN 'LOW_STOCK' " +
           "  ELSE 'IN_STOCK' END, " +
           "i.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE i.productId = :productId AND (i.quantity - i.reservedStock - :qty) >= 0")
    int reserveStock(@Param("productId") Long productId, @Param("qty") Integer qty);

    /** Release reserved stock (order cancelled / fulfilled). Decreases reservedStock. */
    @Modifying
    @Query("UPDATE Inventory i SET " +
           "i.reservedStock = GREATEST(0, i.reservedStock - :qty), " +
           "i.status = CASE " +
           "  WHEN (i.quantity - GREATEST(0, i.reservedStock - :qty)) <= 0 THEN 'OUT_OF_STOCK' " +
           "  WHEN (i.quantity - GREATEST(0, i.reservedStock - :qty)) <= i.lowStockThreshold THEN 'LOW_STOCK' " +
           "  ELSE 'IN_STOCK' END, " +
           "i.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE i.productId = :productId")
    int releaseStock(@Param("productId") Long productId, @Param("qty") Integer qty);

    /** Add incoming stock (supplier shipment en-route). */
    @Modifying
    @Query("UPDATE Inventory i SET i.incomingStock = i.incomingStock + :qty, i.lastUpdated = CURRENT_TIMESTAMP WHERE i.productId = :productId")
    int addIncomingStock(@Param("productId") Long productId, @Param("qty") Integer qty);

    /** Receive incoming stock — moves incomingStock into quantity. */
    @Modifying
    @Query("UPDATE Inventory i SET " +
           "i.quantity = i.quantity + :qty, " +
           "i.incomingStock = GREATEST(0, i.incomingStock - :qty), " +
           "i.status = CASE " +
           "  WHEN (i.quantity + :qty - i.reservedStock) <= 0 THEN 'OUT_OF_STOCK' " +
           "  WHEN (i.quantity + :qty - i.reservedStock) <= i.lowStockThreshold THEN 'LOW_STOCK' " +
           "  ELSE 'IN_STOCK' END, " +
           "i.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE i.productId = :productId")
    int receiveIncomingStock(@Param("productId") Long productId, @Param("qty") Integer qty);
}
