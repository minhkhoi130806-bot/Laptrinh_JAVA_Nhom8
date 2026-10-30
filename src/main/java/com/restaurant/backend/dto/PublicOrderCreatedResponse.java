package com.restaurant.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PublicOrderCreatedResponse(
        Integer id, String status, LocalDateTime createdAt, BigDecimal total,
        String customerName, String customerPhone, String orderType,
        String paymentPreference, List<OrderItemResponse> items
) { }
