
package com.restaurant.backend.dto;

import java.time.LocalDateTime;

// Du lieu gui len khi tao hoac cap nhat don hang
public record OrderRequest(
        Integer customerId,
        Integer tableId,
        Integer employeeId,
        LocalDateTime createdAt,
        String status
) {
}

