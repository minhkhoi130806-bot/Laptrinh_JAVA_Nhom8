package com.restaurant.backend.service;

import com.restaurant.backend.dto.CheckoutRequest;
import com.restaurant.backend.entity.Invoice;

import java.util.List;

/**
 * =========================================================================
 * INTERFACE DỊCH VỤ HÓA ĐƠN & THANH TOÁN (InvoiceService)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Thanh toán Hóa đơn)
 * 
 * Mục đích:
 * Định nghĩa các thao tác quản lý hóa đơn và quy trình thanh toán:
 * 1. API THANH TOÁN TẠO INVOICE (@Transactional):
 *    - Tính tổng tiền các món ăn trong đơn hàng
 *    - Trừ tiền giảm giá (nếu có)
 *    - Tạo hóa đơn lưu phương thức thanh toán và thời gian trả tiền
 *    - Đổi trạng thái đơn hàng sang "PAID"
 *    - Tự động giải phóng bàn ăn (chuyển trạng thái bàn về AVAILABLE)
 * 2. Các hàm xem danh sách hóa đơn, xem chi tiết hóa đơn
 * =========================================================================
 */
public interface InvoiceService {

    /**
     * NGHIỆP VỤ CỐT LÕI TUẦN 3: THANH TOÁN ĐƠN HÀNG VÀ TẠO HÓA ĐƠN
     * Toàn bộ các thao tác được bọc trong một @Transactional để đảm bảo an toàn tuyệt đối.
     * 
     * @param request Chứa orderId, discount, paymentMethod (CASH, BANKING...)
     * @return Hóa đơn thanh toán vừa được tạo thành công
     */
    Invoice checkout(CheckoutRequest request);

    /**
     * Thêm mới một hóa đơn thủ công
     */
    Invoice create(Invoice invoice);

    /**
     * Lấy danh sách toàn bộ các hóa đơn trong hệ thống
     */
    List<Invoice> getAll();

    /**
     * Lấy danh sách hóa đơn theo mã đơn hàng cụ thể
     */
    List<Invoice> getByOrderId(Integer orderId);

    /**
     * Tìm hóa đơn theo mã định danh ID
     */
    Invoice getById(Integer id);

    /**
     * Cập nhật thông tin hóa đơn
     */
    Invoice update(Integer id, Invoice invoice);

    /**
     * Xóa một hóa đơn
     */
    void delete(Integer id);
}
