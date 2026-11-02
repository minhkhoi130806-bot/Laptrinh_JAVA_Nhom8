package com.restaurant.backend.dto;

import java.math.BigDecimal;

public record ZalopayCreateResponse(String appTransId, String orderUrl, BigDecimal amount) { }
