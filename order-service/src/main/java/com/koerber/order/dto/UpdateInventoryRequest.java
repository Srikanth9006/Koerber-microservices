package com.koerber.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInventoryRequest {
    private Long productId;
    private List<BatchUpdateDto> batchUpdates;
}
