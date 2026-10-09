
package com.restaurant.backend.service.Impl;

import com.restaurant.backend.entity.Invoice;
import com.restaurant.backend.repository.InvoiceRepository;
import com.restaurant.backend.service.InvoiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Xu ly nghiep vu hoa don
@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    // Them hoa don
    @Override
    public Invoice create(Invoice invoice) {
        invoice.setId(null);
        return invoiceRepository.save(invoice);
    }

    // Lay danh sach hoa don
    @Override
    public List<Invoice> getAll() {
        return invoiceRepository.findAll();
    }

    // Tim hoa don theo ID
    @Override
    public Invoice getById(Integer id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Khong tim thay hoa don co ID: " + id));
    }

    // Cap nhat hoa don
    @Override
    public Invoice update(Integer id, Invoice invoice) {
        Invoice existingInvoice = getById(id);

        existingInvoice.setOrderId(invoice.getOrderId());
        existingInvoice.setSubtotal(invoice.getSubtotal());
        existingInvoice.setDiscount(invoice.getDiscount());
        existingInvoice.setTotal(invoice.getTotal());
        existingInvoice.setPaymentMethod(invoice.getPaymentMethod());
        existingInvoice.setPaidAt(invoice.getPaidAt());

        return invoiceRepository.save(existingInvoice);
    }

    // Xoa hoa don
    @Override
    public void delete(Integer id) {
        Invoice existingInvoice = getById(id);
        invoiceRepository.delete(existingInvoice);
    }
}

