package com.restaurant.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PublicOrderItemRequest(
        @NotNull(message = "Mã món không được để trống") Integer menuItemId,
        @NotNull(message = "Số lượng không được để trống") @Min(value = 1, message = "Số lượng tối thiểu là 1") Integer quantity,
        String note
) { }
