package com.koerber.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koerber.order.client.InventoryClient;
import com.koerber.order.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryClient inventoryClient;

    @Test
    void placeOrder_shouldReturn201WithOrderDetails() throws Exception {
        InventoryResponse inventory = new InventoryResponse(1002L, "Smartphone", List.of(
                new BatchDto(9L, 29, LocalDate.of(2026, 5, 31)),
                new BatchDto(10L, 83, LocalDate.of(2026, 11, 15))
        ));
        when(inventoryClient.getInventory(1002L)).thenReturn(inventory);
        doNothing().when(inventoryClient).updateInventory(any());

        OrderRequest request = new OrderRequest(1002L, 3);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1002))
                .andExpect(jsonPath("$.productName").value("Smartphone"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.reservedFromBatchIds[0]").value(9))
                .andExpect(jsonPath("$.message").value("Order placed. Inventory reserved."));

        verify(inventoryClient).updateInventory(any(UpdateInventoryRequest.class));
    }

    @Test
    void placeOrder_whenInsufficientInventory_shouldReturn422() throws Exception {
        InventoryResponse inventory = new InventoryResponse(1001L, "Laptop", List.of(
                new BatchDto(1L, 5, LocalDate.of(2026, 6, 25))
        ));
        when(inventoryClient.getInventory(1001L)).thenReturn(inventory);

        OrderRequest request = new OrderRequest(1001L, 100);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void placeOrder_whenQuantityIsZero_shouldReturn400() throws Exception {
        OrderRequest request = new OrderRequest(1001L, 0);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
