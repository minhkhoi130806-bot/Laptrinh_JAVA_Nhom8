package com.restaurant.backend.controller;

import com.restaurant.backend.dto.ReceiptRequest;
import com.restaurant.backend.dto.ReceiptRequest.AuditNoteRequest;
import com.restaurant.backend.dto.ReceiptResponse;
import com.restaurant.backend.dto.ReceiptResponse.PurchaseReportResponse;
import com.restaurant.backend.service.ReceiptService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Phiếu nhập kho: tạo, xem và ghi nhận nhật ký kiểm toán chống gian lận
@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptResponse create(@RequestBody ReceiptRequest request) {
        return receiptService.create(request);
    }

    @GetMapping
    public List<ReceiptResponse> getAll() {
        return receiptService.getAll();
    }

    @GetMapping("/report")
    public PurchaseReportResponse getReport() {
        return receiptService.getPurchaseReport();
    }

    @GetMapping("/{id}")
    public ReceiptResponse getById(@PathVariable Long id) {
        return receiptService.getById(id);
    }

    // Bổ sung ghi chú kiểm toán / giải trình chênh lệch chống gian lận
    @PatchMapping("/{id}/note")
    public ReceiptResponse updateNote(@PathVariable Long id, @RequestBody AuditNoteRequest body) {
        String note = body != null ? body.note() : null;
        return receiptService.updateNote(id, note);
    }
}
