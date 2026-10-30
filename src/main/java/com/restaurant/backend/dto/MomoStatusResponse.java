package com.restaurant.backend.dto;

import java.math.BigDecimal;

/** status: PENDING | SUCCESS | FAILED | EXPIRED. */
public record MomoStatusResponse(String momoOrderId, String status, Integer orderId, BigDecimal amount) { }
