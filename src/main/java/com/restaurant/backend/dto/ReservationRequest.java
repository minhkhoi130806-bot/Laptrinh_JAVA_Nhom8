package com.restaurant.backend.dto;

import java.time.LocalDateTime;

public record ReservationRequest(
        Long customerId,
        Long tableId,
        LocalDateTime reservedAt,
        Integer guestCount
) {
}
