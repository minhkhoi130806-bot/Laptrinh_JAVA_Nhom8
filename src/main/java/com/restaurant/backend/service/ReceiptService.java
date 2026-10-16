package com.restaurant.backend.service;

import com.restaurant.backend.dto.ReceiptRequest;
import com.restaurant.backend.dto.ReceiptResponse;

import java.util.List;

// Phiếu nhập kho: chỉ tạo và xem (không sửa/xóa để không lệch tồn kho & sổ sách)
public interface ReceiptService {
    ReceiptResponse create(ReceiptRequest request);
    List<ReceiptResponse> getAll();
    ReceiptResponse getById(Long id);
}
