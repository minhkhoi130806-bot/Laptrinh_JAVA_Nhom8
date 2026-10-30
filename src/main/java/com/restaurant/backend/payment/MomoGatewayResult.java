package com.restaurant.backend.payment;

/** Kết quả rút gọn từ API tạo giao dịch / truy vấn trạng thái của MoMo. */
public record MomoGatewayResult(int resultCode, Long amount, Long transId, String message, String payUrl) { }
