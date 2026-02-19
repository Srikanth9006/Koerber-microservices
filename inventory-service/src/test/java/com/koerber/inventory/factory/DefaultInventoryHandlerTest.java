package com.koerber.inventory.factory;

import com.koerber.inventory.dto.BatchUpdateDto;
import com.koerber.inventory.dto.UpdateInventoryRequest;
import com.koerber.inventory.model.InventoryBatch;
import com.koerber.inventory.repository.InventoryBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultInventoryHandlerTest {

    @Mock
    private InventoryBatchRepository inventoryBatchRepository;

    private DefaultInventoryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DefaultInventoryHandler(inventoryBatchRepository);
    }

    @Test
    void updateInventory_nullRequest_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> handler.updateInventory(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");

        verifyNoInteractions(inventoryBatchRepository);
    }

    @Test
    void updateInventory_nullBatchUpdates_throwsIllegalArgumentException() {
        UpdateInventoryRequest req = new UpdateInventoryRequest(100L, null);
        assertThatThrownBy(() -> handler.updateInventory(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchUpdates must not be null");

        verifyNoInteractions(inventoryBatchRepository);
    }

    @Test
    void updateInventory_emptyBatchUpdates_noRepoInteraction() {
        UpdateInventoryRequest req = new UpdateInventoryRequest(100L, List.of());
        handler.updateInventory(req);
        verifyNoInteractions(inventoryBatchRepository);
    }

    @Test
    void updateInventory_validSingleBatch_deductsQuantityAndSaves() {
        InventoryBatch batch = new InventoryBatch(5L, 100L, "Gadget", 20, LocalDate.now());
        when(inventoryBatchRepository.findById(5L)).thenReturn(Optional.of(batch));
        when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateInventoryRequest req = new UpdateInventoryRequest(100L, List.of(new BatchUpdateDto(5L, 5)));

        handler.updateInventory(req);

        assertThat(batch.getQuantity()).isEqualTo(15);
        verify(inventoryBatchRepository).save(batch);
    }

    @Test
    void updateInventory_insufficientQuantity_throwsIllegalArgumentException() {
        InventoryBatch batch = new InventoryBatch(5L, 100L, "Gadget", 3, LocalDate.now());
        when(inventoryBatchRepository.findById(5L)).thenReturn(Optional.of(batch));

        UpdateInventoryRequest req = new UpdateInventoryRequest(100L, List.of(new BatchUpdateDto(5L, 5)));

        assertThatThrownBy(() -> handler.updateInventory(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient quantity");

        verify(inventoryBatchRepository, never()).save(any());
    }
}

