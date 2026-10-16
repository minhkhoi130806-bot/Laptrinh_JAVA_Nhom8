package com.restaurant.backend.dto;

import java.math.BigDecimal;

public record ReceiptDetailRequest(
        Long ingredientId,
        BigDecimal quantity,
        BigDecimal unitPrice
) {
}
