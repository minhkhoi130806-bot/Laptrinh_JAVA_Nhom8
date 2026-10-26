package com.restaurant.backend.service;

import com.restaurant.backend.dto.CheckoutRequest;
import com.restaurant.backend.dto.RevenueReportResponse;
import com.restaurant.backend.entity.Invoice;

import java.util.List;

/**
 * =========================================================================
 * INTERFACE DỊCH VỤ HÓA ĐƠN & THANH TOÁN (InvoiceService)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 & Tuần 4 - Nghiệp vụ Thanh toán & Báo cáo)
 * 
 * Mục đích:
 * Định nghĩa các thao tác quản lý hóa đơn, quy trình thanh toán và báo cáo doanh thu:
 * 1. API THANH TOÁN TẠO INVOICE (@Transactional):
 *    - Tính tổng tiền các món ăn trong đơn hàng
 *    - Trừ tiền giảm giá (nếu có)
 *    - Tạo hóa đơn lưu phương thức thanh toán và thời gian trả tiền
 *    - Đổi trạng thái đơn hàng sang "PAID"
 *    - Tự động giải phóng bàn ăn (chuyển trạng thái bàn về AVAILABLE)
 *    - Trừ kho nguyên liệu theo RECIPE khi thanh toán (Tuần 4)
 *    - Cộng điểm tích lũy cho khách hàng (Tuần 4)
 * 2. API BÁO CÁO DOANH THU THEO NGÀY/THÁNG (Tuần 4)
 * 3. Các hàm CRUD hóa đơn
 * =========================================================================
 */
public interface InvoiceService {

    /**
     * NGHIỆP VỤ CỐT LÕI TUẦN 3 & 4: THANH TOÁN ĐƠN HÀNG VÀ TẠO HÓA ĐƠN
     * Toàn bộ các thao tác được bọc trong một @Transactional để đảm bảo an toàn tuyệt đối.
     * 
     * @param request Chứa orderId, discount, paymentMethod (CASH, BANKING...)
     * @return Hóa đơn thanh toán vừa được tạo thành công
     */
    Invoice checkout(CheckoutRequest request);

    /**
     * BÁO CÁO DOANH THU THEO NGÀY / KHOẢNG THỜI GIAN (Tuần 4 - Khôi)
     * 
     * @param days Số ngày gần nhất cần thống kê (mặc định 30 ngày nếu truyền null hoặc <= 0)
     * @return Báo cáo doanh thu chi tiết kèm biểu đồ theo ngày và phương thức thanh toán
     */
    RevenueReportResponse getRevenueReport(Integer days);

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
