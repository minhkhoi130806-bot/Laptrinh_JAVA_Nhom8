package com.restaurant.backend.dto;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        Long itemCount      // số món đang bán trong nhóm (không tính thùng rác)
) {
}
