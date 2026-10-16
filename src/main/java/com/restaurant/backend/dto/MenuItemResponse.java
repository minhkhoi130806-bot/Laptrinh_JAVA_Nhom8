package com.restaurant.backend.dto;

import com.restaurant.backend.entity.Spicelevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}
