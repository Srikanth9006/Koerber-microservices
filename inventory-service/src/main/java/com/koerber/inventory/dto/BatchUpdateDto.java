package com.koerber.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateDto {
    private Long batchId;
    private Integer quantityToDeduct;
}
