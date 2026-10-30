package com.restaurant.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Khách yêu cầu thanh toán MoMo cho đơn đã tạo; số điện thoại dùng để xác nhận đúng chủ đơn. Không nhận số tiền từ client. */
public record MomoCreateRequest(
        @NotNull(message = "Thiếu mã đơn hàng") Integer orderId,
        @Pattern(regexp = "^0[0-9]{9,10}$", message = "Số điện thoại không hợp lệ") String phone
) { }
