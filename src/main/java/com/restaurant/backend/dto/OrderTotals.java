package com.restaurant.backend.dto;

import java.math.BigDecimal;

// Tổng tiền và số món của 1 đơn hàng (tính từ order_item)
public record OrderTotals(BigDecimal total, long itemCount) {
    public static final OrderTotals EMPTY = new OrderTotals(BigDecimal.ZERO, 0);
}
