package com.restaurant.backend.entity;

public enum ReservationStatus {
    PENDING,     // chờ xác nhận
    CONFIRMED,   // đã xác nhận
    CANCELLED,   // đã hủy
    COMPLETED    // đã hoàn tất
}