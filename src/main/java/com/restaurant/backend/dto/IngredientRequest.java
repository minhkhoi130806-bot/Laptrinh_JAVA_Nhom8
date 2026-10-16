package com.restaurant.backend.dto;

import java.math.BigDecimal;

public record IngredientRequest(
        String name,
        String unit,
        BigDecimal stockQty,
        BigDecimal minStock
) {
}
