package com.koerber.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchDto {
    private Long batchId;
    private Integer quantity;
    private LocalDate expiryDate;
}
