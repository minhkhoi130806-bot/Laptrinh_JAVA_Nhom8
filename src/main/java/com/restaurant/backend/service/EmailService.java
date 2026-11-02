package com.restaurant.backend.service;

import com.restaurant.backend.entity.Customer;
import com.restaurant.backend.entity.Invoice;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.Reservation;

import java.util.List;
import java.util.Map;

/** Dịch vụ gửi email thông báo từ hệ thống nhà hàng Han Seoul. */
public interface EmailService {

    /**
     * Gửi email xác nhận thanh toán thành công:
     * - Gửi hóa đơn chi tiết tới email của khách hàng (hoặc tự động tra cứu từ hồ sơ hội viên nếu đơn chưa có email).
     * - Đồng thời gửi thông báo tới email quản trị viên của nhà hàng.
     */
    void sendPaymentSuccessEmail(Order order, Invoice invoice, List<OrderItem> items);

    /**
     * Gửi email chào mừng hội viên khi khách hàng mới đăng ký tài khoản:
     * - Gửi thư chào mừng và giới thiệu nhà hàng/ưu đãi tới Gmail của khách hàng.
     * - Đồng thời gửi thông báo tới email quản trị viên.
     */
    void sendCustomerWelcomeEmail(Customer customer);

    /**
     * Gửi email xác nhận đặt bàn thành công:
     * - Gửi thông tin chi tiết đặt bàn (mã đặt bàn, số bàn, ngày giờ, số khách, ghi chú, lưu ý) tới Gmail của khách hàng.
     * - Đồng thời gửi thông báo tới email quản trị viên của nhà hàng để chuẩn bị bàn.
     */
    void sendReservationConfirmationEmail(Reservation reservation);

    /**
     * Gửi mã OTP xác thực đăng ký hội viên qua Gmail tới khách hàng.
     */
    void sendRegistrationOtpEmail(String toEmail, String fullName, String otpCode);

    /**
     * Gửi email kiểm tra cấu hình SMTP trực tiếp và trả về chi tiết thành công/lỗi.
     */
    Map<String, Object> testSendEmail(String toEmail);

    /**
     * Lấy email quản trị viên mặc định đã cấu hình trong application.properties.
     */
    String getDefaultAdminEmail();
}
