package com.restaurant.backend.dto;

public record SupplierRequest(
        String name,
        String phone,
        String email,
        String address
) {
}
