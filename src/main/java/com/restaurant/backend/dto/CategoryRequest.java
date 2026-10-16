package com.restaurant.backend.dto;

public record CategoryRequest(
        String name,
        String description,
        String imageUrl
) {
}
