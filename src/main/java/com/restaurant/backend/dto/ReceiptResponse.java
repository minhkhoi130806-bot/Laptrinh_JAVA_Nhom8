package com.restaurant.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReceiptResponse(
        Long id,
        Long supplierId,
        String supplierName,
        Long employeeId,
        String employeeName,
        LocalDateTime receiptDate,
        BigDecimal totalAmount,
        String note,
        List<ReceiptDetailResponse> details
) {
}
