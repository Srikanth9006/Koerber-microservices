package com.koerber.inventory.factory;

import com.koerber.inventory.dto.BatchDto;
import com.koerber.inventory.dto.BatchUpdateDto;
import com.koerber.inventory.dto.InventoryResponse;
import com.koerber.inventory.dto.UpdateInventoryRequest;
import com.koerber.inventory.exception.ProductNotFoundException;
import com.koerber.inventory.model.InventoryBatch;
import com.koerber.inventory.repository.InventoryBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of InventoryHandler using FEFO
 * (First Expiry, First Out) strategy.
 */
@Component
@RequiredArgsConstructor
public class DefaultInventoryHandler implements InventoryHandler {

    public static final String HANDLER_TYPE = "DEFAULT";

    private final InventoryBatchRepository inventoryBatchRepository;

    @Override
    public InventoryResponse getInventorySortedByExpiry(Long productId) {
        List<InventoryBatch> batches = inventoryBatchRepository.findByProductIdOrderByExpiryDateAsc(productId);

        if (batches.isEmpty()) {
            throw new ProductNotFoundException("No inventory found for productId: " + productId);
        }

        String productName = batches.get(0).getProductName();
        List<BatchDto> batchDtos = batches.stream()
                .map(b -> new BatchDto(b.getBatchId(), b.getQuantity(), b.getExpiryDate()))
                .collect(Collectors.toList());

        return new InventoryResponse(productId, productName, batchDtos);
    }

    @Override
    @Transactional
    public void updateInventory(UpdateInventoryRequest request) {
        // Validate input: null -> error, empty -> no-op
        if (request == null) {
            throw new IllegalArgumentException("UpdateInventoryRequest must not be null");
        }
        if (request.getBatchUpdates() == null) {
            throw new IllegalArgumentException("batchUpdates must not be null");
        }
        if (request.getBatchUpdates().isEmpty()) {
            // nothing to do
            return;
        }

        for (BatchUpdateDto update : request.getBatchUpdates()) {
            InventoryBatch batch = inventoryBatchRepository.findById(update.getBatchId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Batch not found: " + update.getBatchId()));

            int newQuantity = batch.getQuantity() - update.getQuantityToDeduct();
            if (newQuantity < 0) {
                throw new IllegalArgumentException(
                        "Insufficient quantity in batch: " + update.getBatchId());
            }
            batch.setQuantity(newQuantity);
            inventoryBatchRepository.save(batch);
        }
    }

    @Override
    public String getHandlerType() {
        return HANDLER_TYPE;
    }
}
