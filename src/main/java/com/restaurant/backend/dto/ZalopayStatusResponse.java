package com.restaurant.backend.dto;

import java.math.BigDecimal;

/** status: PENDING | SUCCESS | FAILED | EXPIRED. */
public record ZalopayStatusResponse(String appTransId, String status, Integer orderId, BigDecimal amount, String zpTransId) { }
