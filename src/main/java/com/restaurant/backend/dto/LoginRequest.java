package com.restaurant.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Lớp DTO nhận dữ liệu gửi lên khi người dùng (nhân viên) đăng nhập.
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Bảo mật)
 */
public record LoginRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống")
        String username,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password
) {
}
