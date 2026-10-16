package com.restaurant.backend.controller;

import com.restaurant.backend.dto.ReceiptRequest;
import com.restaurant.backend.dto.ReceiptResponse;
import com.restaurant.backend.service.ReceiptService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Phiếu nhập kho: chỉ tạo + xem
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

    @GetMapping("/{id}")
    public ReceiptResponse getById(@PathVariable Long id) {
        return receiptService.getById(id);
    }
}
