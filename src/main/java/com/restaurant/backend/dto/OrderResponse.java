package com.restaurant.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Integer id, Integer customerId, Integer tableId, Integer employeeId,
        LocalDateTime createdAt, String status, BigDecimal total, Long itemCount,
        String customerName, String customerPhone, String customerEmail,
        String orderType, String deliveryAddress, String customerNote, String paymentPreference
) { }
