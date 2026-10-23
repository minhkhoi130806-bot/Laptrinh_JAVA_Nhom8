package com.restaurant.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReceiptRequest(
        Long supplierId,
        Long employeeId,          // nhân viên lập phiếu (có thể bỏ trống)
        LocalDateTime receiptDate, // bỏ trống = bây giờ
        String note,
        List<ReceiptDetailRequest> details
) {
    public record AuditNoteRequest(String note) {}
}
