package com.restaurant.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Du lieu tra ve cua don hang (kem tong tien va so mon de hien thi danh sach)
public record OrderResponse(
        Integer id,
        Integer customerId,
        Integer tableId,
        Integer employeeId,
        LocalDateTime createdAt,
        String status,
        BigDecimal total,
        Long itemCount
) {
}
