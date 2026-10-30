package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.OrderRequest;
import com.restaurant.backend.dto.OrderResponse;
import com.restaurant.backend.dto.OrderTotals;
import com.restaurant.backend.entity.Order;
import org.springframework.stereotype.Component;

/**
 * =========================================================================
 * LỚP CHUYỂN ĐỔI DỮ LIỆU ĐƠN HÀNG (OrderMapper)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Đơn hàng)
 * 
 * Mục đích:
 * Chuyển đổi qua lại giữa:
 * - OrderRequest (DTO nhận từ Client) -> Order (Entity lưu vào CSDL)
 * - Order (Entity đọc từ CSDL) + OrderTotals (tổng tiền) -> OrderResponse (DTO gửi về Client)
 * Giúp bảo vệ dữ liệu nội bộ của CSDL không bị lộ trực tiếp ra ngoài API.
 * =========================================================================
 */
@Component
public class OrderMapper {

    /**
     * Chuyển dữ liệu DTO từ Client thành đối tượng Entity để lưu vào MySQL
     */
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
        order.setCustomerName(request.customerName());
        order.setCustomerPhone(request.customerPhone());
        order.setCustomerEmail(request.customerEmail());
        order.setOrderType(request.orderType());
        order.setDeliveryAddress(request.deliveryAddress());
        order.setCustomerNote(request.customerNote());
        order.setPaymentPreference(request.paymentPreference());
        return order;
    }

    /**
     * Chuyển đối tượng Entity từ MySQL kèm tổng tiền thành DTO trả về cho giao diện
     */
    public OrderResponse toResponse(Order order, OrderTotals totals) {
        if (order == null) {
            return null;
        }
        OrderTotals t = (totals != null) ? totals : OrderTotals.EMPTY;
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTableId(),
                order.getEmployeeId(),
                order.getCreatedAt(),
                order.getStatus(),
                t.total(),
                t.itemCount(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerEmail(),
                order.getOrderType(),
                order.getDeliveryAddress(),
                order.getCustomerNote(),
                order.getPaymentPreference()
        );
    }

    /**
     * Chuyển Entity sang Response khi chưa cần tính tổng tiền
     */
    public OrderResponse toResponse(Order order) {
        return toResponse(order, null);
    }
}
