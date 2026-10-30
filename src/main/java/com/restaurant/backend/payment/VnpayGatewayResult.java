package com.restaurant.backend.payment;

/** Kết quả rút gọn từ VNPay return/IPN. */
public record VnpayGatewayResult(String responseCode, long amount, String transactionNo, String bankCode, String message) { }
