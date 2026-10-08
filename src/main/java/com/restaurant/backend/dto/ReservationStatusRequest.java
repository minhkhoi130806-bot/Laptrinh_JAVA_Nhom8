package com.restaurant.backend.dto;

import com.restaurant.backend.entity.ReservationStatus;

public record ReservationStatusRequest(ReservationStatus status) {
}
