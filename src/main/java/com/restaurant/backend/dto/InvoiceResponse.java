
package com.restaurant.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Du lieu tra ve cua hoa don
public record InvoiceResponse(
        Integer id,
        Integer orderId,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String paymentMethod,
        LocalDateTime paidAt
) {
}

