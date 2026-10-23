package com.restaurant.backend.service;

import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemId;

import java.util.List;

/**
 * =========================================================================
 * INTERFACE DỊCH VỤ GỌI MÓN / CHI TIẾT ĐƠN HÀNG (OrderItemService)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Gọi món & Chi tiết đơn)
 * 
 * Mục đích:
 * Định nghĩa các thao tác khi khách gọi món hoặc thay đổi món trong đơn:
 * - Thêm món ăn vào đơn hàng (tự động chốt và lưu đơn giá tại thời điểm gọi)
 * - Xem danh sách các món trong 1 đơn hàng
 * - Sửa số lượng hoặc ghi chú của món ăn
 * - Xóa món ăn khỏi đơn hàng
 * =========================================================================
 */
public interface OrderItemService {

    /**
     * Thêm một món ăn vào đơn hàng.
     * Lưu ý đặc biệt (Yêu cầu Tuần 3): Tự động lấy giá hiện tại của món ăn
     * để lưu vào cột unit_price, bảo đảm sau này nếu Menu đổi giá thì đơn cũ không bị đổi.
     * 
     * @param orderItem Thông tin món gồm: orderId, menuItemId, quantity, note...
     * @return Bản ghi chi tiết món đã được lưu vào CSDL
     */
    OrderItem create(OrderItem orderItem);

    /**
     * Lấy toàn bộ danh sách chi tiết các món ăn của tất cả đơn hàng
     */
    List<OrderItem> getAll();

    /**
     * Lấy danh sách các món ăn thuộc về một đơn hàng cụ thể
     * 
     * @param orderId Mã ID của đơn hàng
     * @return Danh sách các món mà khách đã gọi trong đơn đó
     */
    List<OrderItem> getByOrderId(Integer orderId);

    /**
     * Tìm chi tiết món ăn theo khóa chính kép (Mã đơn hàng + Mã món ăn)
     * 
     * @param id Khóa chính kép OrderItemId(orderId, menuItemId)
     * @return Chi tiết món ăn tìm thấy
     */
    OrderItem getById(OrderItemId id);

    /**
     * Cập nhật số lượng hoặc ghi chú của món đã gọi
     * 
     * @param id        Khóa chính kép OrderItemId
     * @param orderItem Dữ liệu mới (số lượng mới, ghi chú mới)
     * @return Chi tiết món ăn sau khi cập nhật
     */
    OrderItem update(OrderItemId id, OrderItem orderItem);

    /**
     * Xóa một món ăn ra khỏi đơn hàng (khách hủy món)
     * 
     * @param id Khóa chính kép OrderItemId(orderId, menuItemId)
     */
    void delete(OrderItemId id);
}
