package com.koerber.inventory.service;

import com.koerber.inventory.dto.BatchUpdateDto;
import com.koerber.inventory.dto.InventoryResponse;
import com.koerber.inventory.dto.UpdateInventoryRequest;
import com.koerber.inventory.exception.ProductNotFoundException;
import com.koerber.inventory.factory.DefaultInventoryHandler;
import com.koerber.inventory.factory.InventoryHandlerFactory;
import com.koerber.inventory.model.InventoryBatch;
import com.koerber.inventory.repository.InventoryBatchRepository;
import com.koerber.inventory.service.impl.DefaultInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryBatchRepository inventoryBatchRepository;

    private DefaultInventoryHandler defaultInventoryHandler;
    private InventoryHandlerFactory inventoryHandlerFactory;
    private DefaultInventoryService inventoryService;

    @BeforeEach
    void setUp() {
        defaultInventoryHandler = new DefaultInventoryHandler(inventoryBatchRepository);
        inventoryHandlerFactory = new InventoryHandlerFactory(List.of(defaultInventoryHandler));
        inventoryService = new DefaultInventoryService(inventoryHandlerFactory);
    }

    @Test
    void getInventorySortedByExpiry_shouldReturnSortedBatches() {
        Long productId = 1005L;
        List<InventoryBatch> batches = List.of(
                new InventoryBatch(5L, productId, "Smartwatch", 39, LocalDate.of(2026, 3, 31)),
                new InventoryBatch(7L, productId, "Smartwatch", 40, LocalDate.of(2026, 4, 24)),
                new InventoryBatch(2L, productId, "Smartwatch", 52, LocalDate.of(2026, 5, 30))
        );
        when(inventoryBatchRepository.findByProductIdOrderByExpiryDateAsc(productId)).thenReturn(batches);

        InventoryResponse response = inventoryService.getInventorySortedByExpiry(productId);

        assertThat(response.getProductId()).isEqualTo(productId);
        assertThat(response.getProductName()).isEqualTo("Smartwatch");
        assertThat(response.getBatches()).hasSize(3);
        assertThat(response.getBatches().get(0).getExpiryDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(response.getBatches().get(1).getExpiryDate()).isEqualTo(LocalDate.of(2026, 4, 24));
    }

    @Test
    void getInventorySortedByExpiry_whenNoInventory_shouldThrowProductNotFoundException() {
        Long productId = 9999L;
        when(inventoryBatchRepository.findByProductIdOrderByExpiryDateAsc(productId))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> inventoryService.getInventorySortedByExpiry(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void updateInventory_shouldDeductQuantityFromBatches() {
        InventoryBatch batch = new InventoryBatch(5L, 1005L, "Smartwatch", 39, LocalDate.of(2026, 3, 31));
        when(inventoryBatchRepository.findById(5L)).thenReturn(Optional.of(batch));
        when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenReturn(batch);

        UpdateInventoryRequest request = new UpdateInventoryRequest(
                1005L, List.of(new BatchUpdateDto(5L, 10))
        );

        inventoryService.updateInventory(request);

        assertThat(batch.getQuantity()).isEqualTo(29);
        verify(inventoryBatchRepository).save(batch);
    }

    @Test
    void updateInventory_whenInsufficientQuantity_shouldThrowException() {
        InventoryBatch batch = new InventoryBatch(5L, 1005L, "Smartwatch", 5, LocalDate.of(2026, 3, 31));
        when(inventoryBatchRepository.findById(5L)).thenReturn(Optional.of(batch));

        UpdateInventoryRequest request = new UpdateInventoryRequest(
                1005L, List.of(new BatchUpdateDto(5L, 10))
        );

        assertThatThrownBy(() -> inventoryService.updateInventory(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient quantity");
    }

    @Test
    void updateInventory_whenBatchNotFound_shouldThrowException() {
        when(inventoryBatchRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateInventoryRequest request = new UpdateInventoryRequest(
                1001L, List.of(new BatchUpdateDto(99L, 5))
        );

        assertThatThrownBy(() -> inventoryService.updateInventory(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Batch not found");
    }
}
