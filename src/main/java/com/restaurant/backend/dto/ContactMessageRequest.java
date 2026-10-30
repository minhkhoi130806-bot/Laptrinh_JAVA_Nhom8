package com.restaurant.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactMessageRequest(
    @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100, message = "Họ tên tối đa 100 ký tự") String name,
    @Email(message = "Email không đúng định dạng") @Size(max = 190) String email,
    @Pattern(regexp = "^[0-9+() .-]{8,20}$", message = "Số điện thoại không hợp lệ") String phone,
    @NotBlank(message = "Vui lòng chọn chủ đề") @Size(max = 180) String subject,
    @NotBlank(message = "Vui lòng nhập nội dung") @Size(min = 10, max = 2000, message = "Nội dung phải từ 10 đến 2000 ký tự") String message
) { }
