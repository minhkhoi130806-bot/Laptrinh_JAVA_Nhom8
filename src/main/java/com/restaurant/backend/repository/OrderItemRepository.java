package com.restaurant.backend.repository;

import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;

/**
 * =========================================================================
 * REPOSITORY CHI TIẾT ĐƠN HÀNG (OrderItemRepository)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Đơn hàng & Gọi món)
 *
 * Mục đích:
 * Thao tác với bảng "order_item" (Lưu danh sách món ăn của từng đơn hàng).
 * Bảng này dùng khóa chính ghép gồm (order_id + menu_item_id) qua OrderItemId.
 * =========================================================================
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemId> {

    /**
     * Lấy danh sách tất cả các món ăn thuộc về 1 đơn hàng cụ thể
     *
     * @param orderId Mã đơn hàng cần xem món
     * @return Danh sách các chi tiết món ăn trong đơn
     */
    List<OrderItem> findByIdOrderId(Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select oi from OrderItem oi where oi.id.orderId = :orderId order by oi.id.menuItemId")
    List<OrderItem> findByOrderIdForUpdate(@Param("orderId") Integer orderId);

    /**
     * Xóa toàn bộ các món của một đơn hàng (dùng khi hủy hoặc xóa đơn hàng)
     */
    void deleteByIdOrderId(Integer orderId);

    /**
     * Tính tổng tiền và tổng số lượng món ăn của nhiều đơn hàng cùng lúc
     * Sử dụng câu lệnh JPQL để tổng hợp nhanh trong 1 lần truy vấn CSDL:
     * Kết quả mỗi dòng: [orderId, tổng tiền (sum(quantity * unitPrice)), tổng số món (sum(quantity))]
     */
    @Query("select oi.id.orderId, sum(oi.quantity * oi.unitPrice), sum(oi.quantity) "
            + "from OrderItem oi where oi.id.orderId in :orderIds group by oi.id.orderId")
    List<Object[]> aggregateByOrderIds(@Param("orderIds") Collection<Integer> orderIds);

    // Chỉ tính đơn đã thanh toán; giá trị món trước giảm giá hóa đơn.
    @Query("select oi.id.menuItemId, sum(oi.quantity), sum(oi.quantity * oi.unitPrice) "
            + "from OrderItem oi where oi.id.orderId in (select o.id from Order o where o.status = 'PAID') "
            + "group by oi.id.menuItemId order by sum(oi.quantity) desc")
    List<Object[]> findBestSellingMenuItems();
}
