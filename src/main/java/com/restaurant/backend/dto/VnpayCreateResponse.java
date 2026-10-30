package com.restaurant.backend.dto;

import java.math.BigDecimal;

public record VnpayCreateResponse(String vnpTxnRef, String payUrl, BigDecimal amount) { }
