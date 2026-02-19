package com.koerber.inventory.service;

import com.koerber.inventory.dto.InventoryResponse;
import com.koerber.inventory.dto.UpdateInventoryRequest;

public interface InventoryService {

    InventoryResponse getInventorySortedByExpiry(Long productId);

    void updateInventory(UpdateInventoryRequest request);
}
