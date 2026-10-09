
package com.restaurant.backend.service;

import com.restaurant.backend.entity.Invoice;

import java.util.List;

// Cac chuc nang xu ly hoa don
public interface InvoiceService {

    // Them hoa don
    Invoice create(Invoice invoice);

    // Lay danh sach hoa don
    List<Invoice> getAll();

    // Tim hoa don theo ID
    Invoice getById(Integer id);

    // Cap nhat hoa don
    Invoice update(Integer id, Invoice invoice);

    // Xoa hoa don
    void delete(Integer id);
}
