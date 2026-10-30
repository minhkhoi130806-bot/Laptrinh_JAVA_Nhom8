package com.restaurant.backend.dto;

import java.math.BigDecimal;

/** status: PENDING | SUCCESS | FAILED | EXPIRED. */
public record VnpayStatusResponse(String vnpTxnRef, String status, Integer orderId, BigDecimal amount, String transactionNo) { }
