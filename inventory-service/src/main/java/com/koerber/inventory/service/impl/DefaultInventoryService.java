package com.koerber.inventory.service.impl;

import com.koerber.inventory.dto.InventoryResponse;
import com.koerber.inventory.dto.UpdateInventoryRequest;
import com.koerber.inventory.factory.InventoryHandlerFactory;
import com.koerber.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultInventoryService implements InventoryService {

    private final InventoryHandlerFactory inventoryHandlerFactory;

    @Override
    public InventoryResponse getInventorySortedByExpiry(Long productId) {
        return inventoryHandlerFactory.getDefaultHandler().getInventorySortedByExpiry(productId);
    }

    @Override
    public void updateInventory(UpdateInventoryRequest request) {
        inventoryHandlerFactory.getDefaultHandler().updateInventory(request);
    }
}
