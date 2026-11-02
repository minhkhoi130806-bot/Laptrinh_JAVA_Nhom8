package com.restaurant.backend.service;

import com.restaurant.backend.entity.Invoice;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.entity.OrderItem;

import java.util.List;

/** Dịch vụ gửi email thông báo từ hệ thống nhà hàng Han Seoul. */
public interface EmailService {

    /**
     * Gửi email xác nhận thanh toán thành công:
     * - Gửi hóa đơn chi tiết tới email của khách hàng (order.getCustomerEmail()).
     * - Đồng thời gửi thông báo tới email quản trị viên của nhà hàng (nếu có cấu hình).
     */
    void sendPaymentSuccessEmail(Order order, Invoice invoice, List<OrderItem> items);
}
