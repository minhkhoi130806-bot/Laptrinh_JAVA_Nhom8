package com.restaurant.backend.dto;

public record CustomerResponse(
        Long id,
        String fullName,
        String phone,
        String email,
        Integer loyaltyPoints
) {
}
