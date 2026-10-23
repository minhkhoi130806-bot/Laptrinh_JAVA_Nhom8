package com.restaurant.backend.service;

import com.restaurant.backend.dto.ReceiptRequest;
import com.restaurant.backend.dto.ReceiptResponse;
import com.restaurant.backend.dto.ReceiptResponse.PurchaseReportResponse;

import java.util.List;

// Phiếu nhập kho: tạo, xem và ghi nhận nhật ký kiểm toán chống gian lận
public interface ReceiptService {
    ReceiptResponse create(ReceiptRequest request);
    List<ReceiptResponse> getAll();
    ReceiptResponse getById(Long id);
    PurchaseReportResponse getPurchaseReport();

    // Bổ sung ghi chú kiểm toán / giải trình sai lệch chống gian lận
    ReceiptResponse updateNote(Long id, String auditNote);
}
