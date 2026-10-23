package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.InvoiceRequest;
import com.restaurant.backend.dto.InvoiceResponse;
import com.restaurant.backend.entity.Invoice;
import org.springframework.stereotype.Component;

/**
 * =========================================================================
 * LỚP CHUYỂN ĐỔI DỮ LIỆU HÓA ĐƠN (InvoiceMapper)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Thanh toán Hóa đơn)
 * 
 * Mục đích:
 * Chuyển đổi dữ liệu hóa đơn giữa:
 * - InvoiceRequest (DTO gửi lên) -> Invoice (Entity lưu CSDL)
 * - Invoice (Entity từ CSDL) -> InvoiceResponse (DTO trả về cho Frontend hiển thị)
 * =========================================================================
 */
@Component
public class InvoiceMapper {

    /**
     * Chuyển dữ liệu DTO gửi lên thành Entity Invoice
     */
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

    /**
     * Chuyển đối tượng Entity Invoice từ CSDL thành DTO trả về cho giao diện
     */
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
