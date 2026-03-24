package com.shop.inventory.service;

import com.shop.inventory.kafka.StockEventProducer;
import com.shop.inventory.kafka.StockUpdateEvent;
import com.shop.inventory.model.Inventory;
import com.shop.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository repository;

    @Mock
    private StockEventProducer producer;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory sampleInventory;

    @BeforeEach
    void setUp() {
        sampleInventory = new Inventory(1L, "Laptop", 50, 10);
    }

    // ── getByProductId ──────────────────────────────────────────────────────

    @Test
    void getByProductId_returnsInventory_whenFound() {
        when(repository.findByProductId(1L)).thenReturn(Optional.of(sampleInventory));

        Inventory result = inventoryService.getByProductId(1L);

        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getProductName()).isEqualTo("Laptop");
        verify(repository).findByProductId(1L);
    }

    @Test
    void getByProductId_throwsException_whenNotFound() {
        when(repository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getByProductId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Inventory not found for productId: 99");
    }

    // ── getLowStockItems ────────────────────────────────────────────────────

    @Test
    void getLowStockItems_returnsLowStockList() {
        Inventory lowStock = new Inventory(2L, "Mouse", 5, 10);
        when(repository.findLowStockItems()).thenReturn(List.of(lowStock));

        List<Inventory> result = inventoryService.getLowStockItems();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("LOW_STOCK");
    }

    // ── updateStock ─────────────────────────────────────────────────────────

    @Test
    void updateStock_returnsTrue_andPublishesEvent_whenSuccessful() {
        when(repository.updateStock(1L, -5)).thenReturn(1);
        when(repository.findByProductId(1L)).thenReturn(Optional.of(sampleInventory));

        boolean result = inventoryService.updateStock(1L, -5, "ORDER_PLACED");

        assertThat(result).isTrue();

        ArgumentCaptor<StockUpdateEvent> captor = ArgumentCaptor.forClass(StockUpdateEvent.class);
        verify(producer).publishStockUpdate(captor.capture());
        assertThat(captor.getValue().getProductId()).isEqualTo(1L);
        assertThat(captor.getValue().getQuantityChange()).isEqualTo(-5);
        assertThat(captor.getValue().getReason()).isEqualTo("ORDER_PLACED");
    }

    @Test
    void updateStock_returnsFalse_whenInsufficientStock() {
        when(repository.updateStock(1L, -100)).thenReturn(0);

        boolean result = inventoryService.updateStock(1L, -100, "ORDER_PLACED");

        assertThat(result).isFalse();
        verify(producer, never()).publishStockUpdate(any());
    }

    @Test
    void updateStock_doesNotPublish_whenInventoryNotFoundAfterUpdate() {
        when(repository.updateStock(1L, 10)).thenReturn(1);
        when(repository.findByProductId(1L)).thenReturn(Optional.empty());

        boolean result = inventoryService.updateStock(1L, 10, "RESTOCK");

        assertThat(result).isTrue();
        verify(producer, never()).publishStockUpdate(any());
    }

    // ── addInventory ────────────────────────────────────────────────────────

    @Test
    void addInventory_setsStatusAndSaves() {
        Inventory newItem = new Inventory(3L, "Keyboard", 100, 15);
        when(repository.save(any(Inventory.class))).thenReturn(newItem);

        Inventory result = inventoryService.addInventory(newItem);

        assertThat(result.getStatus()).isEqualTo("IN_STOCK");
        verify(repository).save(newItem);
    }

    @Test
    void addInventory_setsLowStock_whenQuantityBelowThreshold() {
        Inventory lowItem = new Inventory(4L, "Cable", 3, 10);
        when(repository.save(any(Inventory.class))).thenReturn(lowItem);

        Inventory result = inventoryService.addInventory(lowItem);

        assertThat(result.getStatus()).isEqualTo("LOW_STOCK");
    }

    @Test
    void addInventory_setsOutOfStock_whenQuantityIsZero() {
        Inventory outItem = new Inventory(5L, "Charger", 0, 10);
        when(repository.save(any(Inventory.class))).thenReturn(outItem);

        Inventory result = inventoryService.addInventory(outItem);

        assertThat(result.getStatus()).isEqualTo("OUT_OF_STOCK");
    }

    // ── getAllInventory ──────────────────────────────────────────────────────

    @Test
    void getAllInventory_returnsAllItems() {
        when(repository.findAll()).thenReturn(List.of(sampleInventory));

        List<Inventory> result = inventoryService.getAllInventory();

        assertThat(result).hasSize(1);
        verify(repository).findAll();
    }
}
