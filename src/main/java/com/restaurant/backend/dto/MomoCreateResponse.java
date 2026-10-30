package com.restaurant.backend.dto;

import java.math.BigDecimal;

public record MomoCreateResponse(String momoOrderId, String payUrl, BigDecimal amount) { }
