package com.restaurant.backend.repository;

import com.restaurant.backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================================
 * REPOSITORY HÓA ĐƠN THANH TOÁN (InvoiceRepository)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Thanh toán Hóa đơn)
 * 
 * Mục đích:
 * Thao tác với bảng "invoice" trong cơ sở dữ liệu.
 * Lưu thông tin thanh toán tiền của khách sau khi dùng bữa xong.
 * =========================================================================
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    /**
     * Tìm danh sách hóa đơn theo mã đơn hàng
     * 
     * @param orderId Mã đơn hàng
     * @return Danh sách hóa đơn (thường mỗi đơn hàng có 1 hóa đơn)
     */
    List<Invoice> findByOrderId(Integer orderId);

    /**
     * Tìm hóa đơn đầu tiên của một đơn hàng (nếu có)
     */
    Optional<Invoice> findFirstByOrderId(Integer orderId);
}