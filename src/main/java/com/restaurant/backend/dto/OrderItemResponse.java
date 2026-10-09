
package com.restaurant.backend.dto;

import java.math.BigDecimal;

// Du lieu tra ve cua chi tiet don hang
public record OrderItemResponse(
        Integer orderId,
        Integer menuItemId,
        Integer quantity,
        BigDecimal unitPrice,
        String note
) {
}

