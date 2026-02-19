package com.koerber.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koerber.inventory.dto.BatchUpdateDto;
import com.koerber.inventory.dto.UpdateInventoryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getInventory_shouldReturnBatchesSortedByExpiryDate() throws Exception {
        mockMvc.perform(get("/inventory/1005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1005))
                .andExpect(jsonPath("$.productName").value("Smartwatch"))
                .andExpect(jsonPath("$.batches", hasSize(3)))
                .andExpect(jsonPath("$.batches[0].expiryDate").value("2026-03-31"))
                .andExpect(jsonPath("$.batches[1].expiryDate").value("2026-04-24"))
                .andExpect(jsonPath("$.batches[2].expiryDate").value("2026-05-30"));
    }

    @Test
    void getInventory_shouldReturnSingleBatchForLaptop() throws Exception {
        mockMvc.perform(get("/inventory/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1001))
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.batches", hasSize(1)));
    }

    @Test
    void getInventory_whenProductNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/inventory/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateInventory_shouldDeductQuantitySuccessfully() throws Exception {
        UpdateInventoryRequest request = new UpdateInventoryRequest(
                1001L, List.of(new BatchUpdateDto(1L, 5))
        );

        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify quantity was reduced
        mockMvc.perform(get("/inventory/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batches[0].quantity").value(63));
    }
}
