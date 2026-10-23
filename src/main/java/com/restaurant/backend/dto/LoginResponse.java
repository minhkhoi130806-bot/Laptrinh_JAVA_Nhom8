package com.restaurant.backend.dto;

/**
 * Lớp DTO trả về kết quả sau khi đăng nhập thành công.
 * Trả về chuỗi JWT token và thông tin cơ bản của nhân viên để giao diện lưu trữ.
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Bảo mật)
 */
public record LoginResponse(
        String token,
        Long id,
        String username,
        String fullName,
        String role,
        String accountType
) {
}
