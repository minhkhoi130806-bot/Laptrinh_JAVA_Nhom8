package com.restaurant.backend.dto;

import java.math.BigDecimal;

public record ReceiptDetailResponse(
        Long ingredientId,
        String ingredientName,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
