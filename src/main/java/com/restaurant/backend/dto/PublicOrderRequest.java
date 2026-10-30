package com.restaurant.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PublicOrderRequest(
        @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100) String customerName,
        @NotBlank(message = "Vui lòng nhập số điện thoại") @Pattern(regexp = "^0[0-9]{9,10}$", message = "Số điện thoại không hợp lệ") String customerPhone,
        @Email(message = "Email không hợp lệ") @Size(max = 150) String customerEmail,
        @NotBlank(message = "Vui lòng chọn hình thức nhận món") String orderType,
        @Size(max = 500) String deliveryAddress,
        @Size(max = 1000) String customerNote,
        @NotBlank(message = "Vui lòng chọn phương thức thanh toán") String paymentPreference,
        @Valid List<PublicOrderItemRequest> items
) { }
