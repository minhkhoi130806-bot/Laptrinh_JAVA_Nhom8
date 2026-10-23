package com.restaurant.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record IngredientResponse(
        Long id,
        String name,
        String unit,
        BigDecimal stockQty,
        BigDecimal minStock,
        Boolean lowStock     // true khi tồn kho <= mức tối thiểu
) {
    public record InventoryReportResponse(
            long totalIngredients,
            long lowStockCount,
            long outOfStockCount,
            List<IngredientResponse> ingredients
    ) {}
}
