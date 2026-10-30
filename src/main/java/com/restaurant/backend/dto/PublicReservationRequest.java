package com.restaurant.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Yêu cầu đặt bàn từ trang công khai của khách hàng.
 * Không yêu cầu khách biết customerId; hệ thống tìm/tạo khách bằng số điện thoại.
 */
public record PublicReservationRequest(
        @NotBlank(message = "Vui lòng nhập họ tên")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String fullName,

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(regexp = "^[0-9+() .-]{8,15}$", message = "Số điện thoại không hợp lệ")
        String phone,

        @Email(message = "Email không hợp lệ")
        @Size(max = 100, message = "Email tối đa 100 ký tự")
        String email,

        @NotNull(message = "Vui lòng chọn ngày giờ")
        @Future(message = "Thời gian đặt bàn phải ở tương lai")
        LocalDateTime reservedAt,

        @NotNull(message = "Vui lòng nhập số khách")
        @Min(value = 1, message = "Số khách tối thiểu là 1")
        @Max(value = 30, message = "Số khách tối đa là 30")
        Integer guestCount,

        @NotNull(message = "Vui lòng chọn bàn")
        Long tableId,

        @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
        String note
) {
}
