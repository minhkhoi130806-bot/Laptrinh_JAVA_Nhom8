package com.restaurant.backend.dto;

import com.restaurant.backend.entity.Spicelevel;

import java.math.BigDecimal;

public record MenuItemRequest(
        Long categoryId,
        String nameVi,
        String nameKo,
        String description,
        BigDecimal price,
        String imageUrl,
        Spicelevel spiceLevel,
        Boolean available
) {
}
