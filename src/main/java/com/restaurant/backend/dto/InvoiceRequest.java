
package com.restaurant.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Du lieu gui len khi them hoac cap nhat hoa don
public record InvoiceRequest(
        Integer orderId,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String paymentMethod,
        LocalDateTime paidAt
) {
}

