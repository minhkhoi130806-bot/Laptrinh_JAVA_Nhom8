package com.restaurant.backend.dto;

public record CustomerRequest(
        String fullName,
        String phone,
        String email,
        Integer loyaltyPoints   // chỉ admin chỉnh khi sửa; bỏ trống = giữ nguyên
) {
}
