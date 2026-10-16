package com.restaurant.backend.dto;

import java.math.BigDecimal;

public record RecipeLineResponse(
        Long ingredientId,
        String ingredientName,
        String unit,
        BigDecimal quantityNeeded
) {
}
