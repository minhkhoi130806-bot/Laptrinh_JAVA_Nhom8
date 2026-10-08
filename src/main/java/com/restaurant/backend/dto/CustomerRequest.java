package com.restaurant.backend.dto;

public record CustomerRequest(
        String fullName,
        String phone,
        String email
) {
}
