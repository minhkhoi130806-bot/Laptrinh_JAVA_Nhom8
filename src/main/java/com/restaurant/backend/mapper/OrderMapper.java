
package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.OrderRequest;
import com.restaurant.backend.dto.OrderResponse;
import com.restaurant.backend.entity.Order;
import org.springframework.stereotype.Component;

// Chuyen doi du lieu don hang
@Component
public class OrderMapper {

    // Chuyen Request thanh Entity
    public Order toEntity(OrderRequest request) {
        if (request == null) {
            return null;
        }

        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setTableId(request.tableId());
        order.setEmployeeId(request.employeeId());
        order.setCreatedAt(request.createdAt());
        order.setStatus(request.status());

        return order;
    }

    // Chuyen Entity thanh Response
    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTableId(),
                order.getEmployeeId(),
                order.getCreatedAt(),
                order.getStatus()
        );
    }
}

