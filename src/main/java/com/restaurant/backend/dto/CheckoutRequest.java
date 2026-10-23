package com.restaurant.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Lớp DTO nhận dữ liệu gửi lên khi thu ngân bấm nút "Thanh toán" cho đơn hàng.
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ thanh toán tạo Invoice)
 */
public record CheckoutRequest(
        // Mã đơn hàng cần thanh toán
        @NotNull(message = "Mã đơn hàng không được để trống")
        Integer orderId,

        // Tiền giảm giá (nếu có, ví dụ: 20000 VNĐ). Nếu không truyền mặc định là 0
        BigDecimal discount,

        // Phương thức thanh toán: CASH (Tiền mặt), BANKING (Chuyển khoản), CARD (Quẹt thẻ)
        String paymentMethod
) {
}
