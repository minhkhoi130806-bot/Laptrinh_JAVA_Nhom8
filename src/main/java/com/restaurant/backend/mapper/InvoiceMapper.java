
package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.InvoiceRequest;
import com.restaurant.backend.dto.InvoiceResponse;
import com.restaurant.backend.entity.Invoice;
import org.springframework.stereotype.Component;

// Chuyen doi du lieu hoa don
@Component
public class InvoiceMapper {

    // Chuyen Request thanh Entity
    public Invoice toEntity(InvoiceRequest request) {
        if (request == null) {
            return null;
        }

        Invoice invoice = new Invoice();
        invoice.setOrderId(request.orderId());
        invoice.setSubtotal(request.subtotal());
        invoice.setDiscount(request.discount());
        invoice.setTotal(request.total());
        invoice.setPaymentMethod(request.paymentMethod());
        invoice.setPaidAt(request.paidAt());

        return invoice;
    }

    // Chuyen Entity thanh Response
    public InvoiceResponse toResponse(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getOrderId(),
                invoice.getSubtotal(),
                invoice.getDiscount(),
                invoice.getTotal(),
                invoice.getPaymentMethod(),
                invoice.getPaidAt()
        );
    }
}

