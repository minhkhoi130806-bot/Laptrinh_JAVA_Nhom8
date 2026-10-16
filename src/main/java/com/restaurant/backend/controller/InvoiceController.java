
package com.restaurant.backend.controller;

import com.restaurant.backend.dto.InvoiceRequest;
import com.restaurant.backend.dto.InvoiceResponse;
import com.restaurant.backend.entity.Invoice;
import com.restaurant.backend.mapper.InvoiceMapper;
import com.restaurant.backend.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Quan ly API hoa don
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceMapper invoiceMapper;

    public InvoiceController(
            InvoiceService invoiceService,
            InvoiceMapper invoiceMapper) {
        this.invoiceService = invoiceService;
        this.invoiceMapper = invoiceMapper;
    }

    // Them hoa don
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@RequestBody InvoiceRequest request) {
        return invoiceMapper.toResponse(
                invoiceService.create(invoiceMapper.toEntity(request))
        );
    }

    // Lay danh sach hoa don: GET /api/invoices?orderId=1
    @GetMapping
    public List<InvoiceResponse> getAll(@RequestParam(required = false) Integer orderId) {
        List<Invoice> invoices = orderId != null
                ? invoiceService.getByOrderId(orderId)
                : invoiceService.getAll();
        return invoices.stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    // Tim hoa don theo ID
    @GetMapping("/{id}")
    public InvoiceResponse getById(@PathVariable Integer id) {
        return invoiceMapper.toResponse(invoiceService.getById(id));
    }

    // Cap nhat hoa don
    @PutMapping("/{id}")
    public InvoiceResponse update(
            @PathVariable Integer id,
            @RequestBody InvoiceRequest request) {
        return invoiceMapper.toResponse(
                invoiceService.update(id, invoiceMapper.toEntity(request))
        );
    }

    // Xoa hoa don
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        invoiceService.delete(id);
    }
}

