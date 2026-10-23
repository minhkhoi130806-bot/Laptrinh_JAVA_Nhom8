package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.OrderItemRequest;
import com.restaurant.backend.dto.OrderItemResponse;
import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemId;
import org.springframework.stereotype.Component;

/**
 * =========================================================================
 * LỚP CHUYỂN ĐỔI DỮ LIỆU GỌI MÓN (OrderItemMapper)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Gọi món & Chi tiết đơn)
 * 
 * Mục đích:
 * Chuyển đổi dữ liệu chi tiết món ăn giữa DTO và Entity:
 * - OrderItemRequest -> OrderItem (ghép orderId và menuItemId thành khóa chính kép OrderItemId)
 * - OrderItem -> OrderItemResponse (tách phẳng các trường để gửi về cho giao diện dễ hiển thị)
 * =========================================================================
 */
@Component
public class OrderItemMapper {

    /**
     * Chuyển dữ liệu DTO gửi lên thành Entity OrderItem
     */
    public OrderItem toEntity(OrderItemRequest request) {
        if (request == null) {
            return null;
        }

        OrderItem orderItem = new OrderItem();

        // Khởi tạo khóa chính kép gồm orderId và menuItemId
        OrderItemId id = new OrderItemId(
                request.orderId(),
                request.menuItemId()
        );

        orderItem.setId(id);
        orderItem.setQuantity(request.quantity());
        orderItem.setUnitPrice(request.unitPrice());
        orderItem.setNote(request.note());

        return orderItem;
    }

    /**
     * Chuyển Entity OrderItem thành DTO Response phẳng
     */
    public OrderItemResponse toResponse(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }

        return new OrderItemResponse(
                orderItem.getId().getOrderId(),
                orderItem.getId().getMenuItemId(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getNote()
        );
    }
}
