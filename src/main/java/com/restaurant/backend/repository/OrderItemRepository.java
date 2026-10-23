package com.restaurant.backend.repository;

import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

// Repository thao tac voi bang order_item
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemId> {

    // Cac mon cua 1 don hang
    List<OrderItem> findByIdOrderId(Integer orderId);

    // Moi dong: [orderId, tong tien, tong so luong] cua tung don
    @Query("select oi.id.orderId, sum(oi.quantity * oi.unitPrice), sum(oi.quantity) "
            + "from OrderItem oi where oi.id.orderId in :orderIds group by oi.id.orderId")
    List<Object[]> aggregateByOrderIds(@Param("orderIds") Collection<Integer> orderIds);

    // Thống kê món bán chạy: [menuItemId, sum(quantity), sum(quantity * unitPrice)]
    @Query("select oi.id.menuItemId, sum(oi.quantity), sum(oi.quantity * oi.unitPrice) "
            + "from OrderItem oi group by oi.id.menuItemId order by sum(oi.quantity) desc")
    List<Object[]> findBestSellingMenuItems();
}
