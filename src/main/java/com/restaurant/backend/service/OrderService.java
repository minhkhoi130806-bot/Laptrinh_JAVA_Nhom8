package com.restaurant.backend.service;

import com.restaurant.backend.dto.OrderTotals;
import com.restaurant.backend.entity.Order;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * =========================================================================
 * INTERFACE DỊCH VỤ ĐƠN HÀNG (OrderService)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Đơn hàng)
 * 
 * Mục đích:
 * Định nghĩa các hành động nghiệp vụ liên quan đến đơn hàng của nhà hàng:
 * - Mở đơn hàng mới (khi khách vào ngồi bàn)
 * - Xem danh sách đơn hàng (lọc theo khách hàng, theo trạng thái)
 * - Xem chi tiết 1 đơn hàng
 * - Đổi trạng thái đơn hàng (OPEN, SERVING, PAID, CANCELLED)
 * - Xóa đơn hàng
 * - Tính tổng tiền của đơn hàng
 * =========================================================================
 */
public interface OrderService {

    /**
     * Tạo một đơn hàng mới cho khách hàng hoặc bàn ăn
     * 
     * @param order Đối tượng đơn hàng chứa customerId, tableId, employeeId
     * @return Đơn hàng vừa được lưu vào CSDL
     */
    Order create(Order order);

    /**
     * Lấy danh sách các đơn hàng, có thể lọc theo mã khách hàng hoặc trạng thái
     * 
     * @param customerId Mã khách hàng (nếu có)
     * @param status     Trạng thái đơn (ví dụ: OPEN, PAID)
     * @return Danh sách đơn hàng phù hợp
     */
    List<Order> getAll(Integer customerId, String status);

    /**
     * Tìm kiếm một đơn hàng theo mã định danh ID
     * 
     * @param id Mã ID của đơn hàng
     * @return Đơn hàng tìm thấy (nếu không thấy sẽ báo lỗi 404)
     */
    Order getById(Integer id);

    /**
     * Cập nhật thông tin của một đơn hàng đã tồn tại
     * 
     * @param id    Mã ID của đơn hàng cần cập nhật
     * @param order Dữ liệu mới cần cập nhật
     * @return Đơn hàng sau khi đã cập nhật
     */
    Order update(Integer id, Order order);

    /**
     * Thay đổi trạng thái của đơn hàng (ví dụ từ OPEN sang SERVING hoặc PAID)
     * 
     * @param id     Mã ID của đơn hàng
     * @param status Trạng thái mới ("OPEN", "SERVING", "PAID", "CANCELLED")
     * @return Đơn hàng với trạng thái mới
     */
    Order updateStatus(Integer id, String status);

    /**
     * Xóa hoàn toàn một đơn hàng khỏi hệ thống
     * 
     * @param id Mã ID đơn hàng cần xóa
     */
    void delete(Integer id);

    /**
     * Tính tổng số tiền và tổng số lượng món ăn của danh sách đơn hàng
     * 
     * @param orderIds Danh sách mã ID các đơn hàng cần tính
     * @return Bảng ánh xạ Map<orderId, OrderTotals> chứa tổng tiền và số món
     */
    Map<Integer, OrderTotals> totals(Collection<Integer> orderIds);
}
