package com.koerber.order.service.impl;

import com.koerber.order.client.InventoryClient;
import com.koerber.order.dto.*;
import com.koerber.order.exception.InsufficientInventoryException;
import com.koerber.order.model.Order;
import com.koerber.order.model.OrderStatus;
import com.koerber.order.repository.OrderRepository;
import com.koerber.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultOrderService implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    @Override
    public OrderResponse placeOrder(OrderRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Order quantity must be greater than zero.");
        }

        // 1. Fetch inventory sorted by expiry (FEFO: First Expiry, First Out)
        InventoryResponse inventory = inventoryClient.getInventory(request.getProductId());

        // 2. Calculate which batches to deduct from using FEFO strategy
        List<BatchDto> batches = inventory.getBatches(); // already sorted by expiry ASC
        int remainingToReserve = request.getQuantity();
        List<Long> reservedBatchIds = new ArrayList<>();
        List<BatchUpdateDto> batchUpdates = new ArrayList<>();

        for (BatchDto batch : batches) {
            if (remainingToReserve <= 0) break;
            if (batch.getQuantity() <= 0) continue;

            int deductAmount = Math.min(batch.getQuantity(), remainingToReserve);
            remainingToReserve -= deductAmount;
            reservedBatchIds.add(batch.getBatchId());
            batchUpdates.add(new BatchUpdateDto(batch.getBatchId(), deductAmount));
        }

        if (remainingToReserve > 0) {
            throw new InsufficientInventoryException(
                    "Insufficient inventory for productId: " + request.getProductId()
                    + ". Requested: " + request.getQuantity()
                    + ", Available: " + (request.getQuantity() - remainingToReserve));
        }

        // 3. Update inventory
        inventoryClient.updateInventory(new UpdateInventoryRequest(request.getProductId(), batchUpdates));

        // 4. Persist the order
        String reservedBatchIdsStr = reservedBatchIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setProductName(inventory.getProductName());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.PLACED);
        order.setOrderDate(LocalDate.now());
        order.setReservedBatchIds(reservedBatchIdsStr);
        Order savedOrder = orderRepository.save(order);

        return OrderResponse.builder()
                .orderId(savedOrder.getOrderId())
                .productId(savedOrder.getProductId())
                .productName(savedOrder.getProductName())
                .quantity(savedOrder.getQuantity())
                .status(savedOrder.getStatus().name())
                .reservedFromBatchIds(reservedBatchIds)
                .message("Order placed. Inventory reserved.")
                .build();
    }
}
