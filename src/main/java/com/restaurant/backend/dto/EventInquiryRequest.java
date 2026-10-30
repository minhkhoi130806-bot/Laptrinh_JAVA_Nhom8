package com.restaurant.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EventInquiryRequest(
    @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100) String name,
    @NotBlank(message = "Vui lòng nhập số điện thoại") @Pattern(regexp = "^[0-9+() .-]{8,20}$", message = "Số điện thoại không hợp lệ") String phone,
    @Email(message = "Email không đúng định dạng") @Size(max = 190) String email,
    @NotBlank(message = "Vui lòng chọn loại tiệc") @Size(max = 60) String event,
    LocalDate date,
    @NotNull(message = "Vui lòng nhập số khách") @Min(value = 2, message = "Tiệc cần ít nhất 2 khách") @Max(value = 200, message = "Tối đa 200 khách") Integer guests,
    @Size(max = 60) String budget,
    @Size(max = 1000) String note
) { }
