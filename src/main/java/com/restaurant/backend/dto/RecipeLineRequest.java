package com.restaurant.backend.dto;

import java.math.BigDecimal;

// 1 dòng công thức: cần bao nhiêu nguyên liệu cho 1 phần món
public record RecipeLineRequest(
        Long ingredientId,
        BigDecimal quantityNeeded
) {
}
