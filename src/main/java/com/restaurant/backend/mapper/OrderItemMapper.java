
package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.OrderItemRequest;
import com.restaurant.backend.dto.OrderItemResponse;
import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemId;
import org.springframework.stereotype.Component;

// Chuyen doi du lieu chi tiet don hang
@Component
public class OrderItemMapper {

    // Chuyen Request thanh Entity
    public OrderItem toEntity(OrderItemRequest request) {
        if (request == null) {
            return null;
        }

        OrderItem orderItem = new OrderItem();

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

    // Chuyen Entity thanh Response
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

