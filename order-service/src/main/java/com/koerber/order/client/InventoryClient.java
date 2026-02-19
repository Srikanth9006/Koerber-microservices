package com.koerber.order.client;

import com.koerber.order.dto.InventoryResponse;
import com.koerber.order.dto.UpdateInventoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    public InventoryResponse getInventory(Long productId) {
        String url = inventoryServiceUrl + "/inventory/" + productId;
        return restTemplate.getForObject(url, InventoryResponse.class);
    }

    public void updateInventory(UpdateInventoryRequest request) {
        String url = inventoryServiceUrl + "/inventory/update";
        restTemplate.postForEntity(url, request, Void.class);
    }
}
