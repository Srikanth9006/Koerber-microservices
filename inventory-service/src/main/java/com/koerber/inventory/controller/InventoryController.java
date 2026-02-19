package com.koerber.inventory.controller;

import com.koerber.inventory.dto.InventoryResponse;
import com.koerber.inventory.dto.UpdateInventoryRequest;
import com.koerber.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management endpoints")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory batches for a product sorted by expiry date")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventorySortedByExpiry(productId));
    }

    @PostMapping("/update")
    @Operation(summary = "Update inventory quantities after an order is placed")
    public ResponseEntity<Void> updateInventory(@RequestBody UpdateInventoryRequest request) {
        inventoryService.updateInventory(request);
        return ResponseEntity.ok().build();
    }
}
