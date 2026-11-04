package com.restaurant.backend.repository;

import com.restaurant.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

/**
 * =========================================================================
 * REPOSITORY QUẢN LÝ ĐƠN HÀNG (OrderRepository)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Đơn hàng)
 * 
 * Mục đích:
 * Cung cấp các hàm truy vấn dữ liệu từ bảng "orders" trong CSDL MySQL.
 * Kế thừa JpaRepository giúp có sẵn các hàm CRUD cơ bản (save, findById, findAll, deleteById...).
 * =========================================================================
 */
public interface OrderRepository extends JpaRepository<Order, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Integer id);

    boolean existsByTableIdAndStatusIn(Integer tableId, Collection<String> statuses);

    /**
     * Tìm danh sách đơn hàng theo mã khách hàng (sắp xếp đơn mới nhất lên đầu)
     */
    List<Order> findByCustomerIdOrderByIdDesc(Integer customerId);

    /**
     * Tìm danh sách đơn hàng theo số điện thoại khách hàng (sắp xếp đơn mới nhất lên đầu)
     */
    List<Order> findByCustomerPhoneOrderByIdDesc(String customerPhone);

    /**
     * Tìm các đơn hàng theo trạng thái cụ thể (ví dụ: "OPEN", "PAID"...)
     */
    List<Order> findByStatus(String status);

    /**
     * Tìm đơn hàng của một bàn đang ở trạng thái chỉ định (ví dụ tìm đơn đang mở của bàn số 5)
     */
    List<Order> findByTableIdAndStatus(Integer tableId, String status);
}
