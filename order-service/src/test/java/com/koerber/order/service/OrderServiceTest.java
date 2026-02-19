package com.koerber.order.service;

import com.koerber.order.client.InventoryClient;
import com.koerber.order.dto.*;
import com.koerber.order.exception.InsufficientInventoryException;
import com.koerber.order.model.Order;
import com.koerber.order.model.OrderStatus;
import com.koerber.order.repository.OrderRepository;
import com.koerber.order.service.impl.DefaultOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    private DefaultOrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new DefaultOrderService(orderRepository, inventoryClient);
    }

    @Test
    void placeOrder_shouldReserveFromEarliestExpiryBatchFirst() {
        OrderRequest request = new OrderRequest(1005L, 50);

        InventoryResponse inventory = new InventoryResponse(1005L, "Smartwatch", List.of(
                new BatchDto(5L, 39, LocalDate.of(2026, 3, 31)),
                new BatchDto(7L, 40, LocalDate.of(2026, 4, 24)),
                new BatchDto(2L, 52, LocalDate.of(2026, 5, 30))
        ));
        when(inventoryClient.getInventory(1005L)).thenReturn(inventory);
        doNothing().when(inventoryClient).updateInventory(any());

        Order savedOrder = new Order(11L, 1005L, "Smartwatch", 50, OrderStatus.PLACED, LocalDate.now(), "5,7");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.placeOrder(request);

        assertThat(response.getStatus()).isEqualTo("PLACED");
        assertThat(response.getQuantity()).isEqualTo(50);
        assertThat(response.getReservedFromBatchIds()).containsExactly(5L, 7L);

        // Verify inventory updated with correct FEFO deductions
        verify(inventoryClient).updateInventory(argThat(req ->
                req.getBatchUpdates().size() == 2 &&
                req.getBatchUpdates().get(0).getBatchId().equals(5L) &&
                req.getBatchUpdates().get(0).getQuantityToDeduct() == 39 &&
                req.getBatchUpdates().get(1).getBatchId().equals(7L) &&
                req.getBatchUpdates().get(1).getQuantityToDeduct() == 11
        ));
    }

    @Test
    void placeOrder_whenSingleBatchSuffices_shouldUseOneBatch() {
        OrderRequest request = new OrderRequest(1001L, 10);

        InventoryResponse inventory = new InventoryResponse(1001L, "Laptop", List.of(
                new BatchDto(1L, 68, LocalDate.of(2026, 6, 25))
        ));
        when(inventoryClient.getInventory(1001L)).thenReturn(inventory);
        doNothing().when(inventoryClient).updateInventory(any());

        Order savedOrder = new Order(11L, 1001L, "Laptop", 10, OrderStatus.PLACED, LocalDate.now(), "1");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.placeOrder(request);

        assertThat(response.getReservedFromBatchIds()).containsExactly(1L);
        assertThat(response.getMessage()).isEqualTo("Order placed. Inventory reserved.");
    }

    @Test
    void placeOrder_whenInsufficientInventory_shouldThrowException() {
        OrderRequest request = new OrderRequest(1005L, 200);

        InventoryResponse inventory = new InventoryResponse(1005L, "Smartwatch", List.of(
                new BatchDto(5L, 39, LocalDate.of(2026, 3, 31)),
                new BatchDto(7L, 40, LocalDate.of(2026, 4, 24)),
                new BatchDto(2L, 52, LocalDate.of(2026, 5, 30))
        ));
        when(inventoryClient.getInventory(1005L)).thenReturn(inventory);

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Insufficient inventory");
    }

    @Test
    void placeOrder_whenQuantityIsZero_shouldThrowIllegalArgumentException() {
        OrderRequest request = new OrderRequest(1001L, 0);

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void placeOrder_whenQuantityIsNegative_shouldThrowIllegalArgumentException() {
        OrderRequest request = new OrderRequest(1001L, -5);

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
