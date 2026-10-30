package com.restaurant.backend.dto;

import java.time.LocalDateTime;

// DTO nhận đơn đặt món; giá tiền luôn được lấy lại từ database theo từng món.
public record OrderRequest(
        Integer customerId,
        Integer tableId,
        Integer employeeId,
        LocalDateTime createdAt,
        String status,
        String customerName,
        String customerPhone,
        String customerEmail,
        String orderType,
        String deliveryAddress,
        String customerNote,
        String paymentPreference
) { }
