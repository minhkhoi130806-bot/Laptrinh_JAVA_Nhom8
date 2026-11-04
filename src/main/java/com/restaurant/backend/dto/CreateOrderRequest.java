package com.restaurant.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateOrderRequest(
        @Positive Integer customerId,
        @NotNull @Positive Integer tableId,
        @Positive Integer employeeId,
        @NotEmpty(message = "Đơn hàng phải có món") @Size(max = 50)
        List<@NotNull @Valid Line> items
) {
    public record Line(
            @NotNull @Positive Integer menuItemId,
            @NotNull @Min(1) @Max(1000) Integer quantity,
            @Size(max = 1000) String note
    ) {}
}
