package com.restaurant.backend.dto;

import java.math.BigDecimal;

public record IngredientResponse(
        Long id,
        String name,
        String unit,
        BigDecimal stockQty,
        BigDecimal minStock,
        Boolean lowStock     // true khi tồn kho <= mức tối thiểu
) {
}
