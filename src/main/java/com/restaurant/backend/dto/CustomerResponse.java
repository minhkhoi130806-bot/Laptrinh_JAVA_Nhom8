package com.restaurant.backend.dto;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String fullName,
        String phone,
        String email,
        Integer loyaltyPoints,
        Boolean deleted,
        LocalDateTime deletedAt
) {
}
