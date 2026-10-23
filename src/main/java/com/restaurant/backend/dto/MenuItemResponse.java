package com.restaurant.backend.dto;

import com.restaurant.backend.entity.Spicelevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MenuItemResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String nameVi,
        String nameKo,
        String description,
        BigDecimal price,
        String imageUrl,
        Spicelevel spiceLevel,
        Boolean available,
        Boolean deleted,
        LocalDateTime deletedAt
) {
    public record BestSellerResponse(
            Long menuItemId,
            String nameVi,
            String nameKo,
            BigDecimal price,
            String categoryName,
            Long totalQuantitySold,
            BigDecimal totalRevenue
    ) {}

    public record MenuItemStockStatus(
            Long menuItemId,
            String nameVi,
            String nameKo,
            Boolean currentlyAvailable,
            Boolean hasEnoughStock,
            List<String> missingIngredients
    ) {}
}
