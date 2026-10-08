package com.restaurant.backend.dto;

import com.restaurant.backend.entity.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long customerId,
        Long tableId,
        LocalDateTime reservedAt,
        Integer guestCount,
        ReservationStatus status
) {
}
