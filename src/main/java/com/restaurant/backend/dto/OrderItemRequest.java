
package com.restaurant.backend.dto;

import java.math.BigDecimal;

// Du lieu gui len khi them hoac cap nhat chi tiet don hang
public record OrderItemRequest(
        Integer orderId,
        Integer menuItemId,
        Integer quantity,
        BigDecimal unitPrice,
        String note
) {
}

