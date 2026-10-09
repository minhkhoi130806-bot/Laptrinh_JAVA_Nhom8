
package com.restaurant.backend.dto;

import java.time.LocalDateTime;

// Du lieu tra ve cua don hang
public record OrderResponse(
        Integer id,
        Integer customerId,
        Integer tableId,
        Integer employeeId,
        LocalDateTime createdAt,
        String status
) {
}

