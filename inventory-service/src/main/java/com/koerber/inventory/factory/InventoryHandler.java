package com.koerber.inventory.factory;

import com.koerber.inventory.dto.InventoryResponse;
import com.koerber.inventory.dto.UpdateInventoryRequest;

/**
 * Strategy interface for inventory handling operations.
 * Implementations can provide different inventory management strategies
 * (e.g., FEFO, FIFO, LIFO) making the system easily extensible.
 */
public interface InventoryHandler {

    /**
     * Retrieves inventory batches for a product sorted by expiry date (ascending).
     *
     * @param productId the product identifier
     * @return InventoryResponse containing the product details and sorted batches
     */
    InventoryResponse getInventorySortedByExpiry(Long productId);

    /**
     * Updates inventory quantities after batches have been reserved for an order.
     *
     * @param request the update request containing batch deduction details
     */
    void updateInventory(UpdateInventoryRequest request);

    /**
     * Returns the handler type key used by the factory for lookup.
     *
     * @return handler type identifier
     */
    String getHandlerType();
}
