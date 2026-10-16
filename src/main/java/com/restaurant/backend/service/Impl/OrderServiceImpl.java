package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.OrderTotals;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.repository.OrderItemRepository;
import com.restaurant.backend.repository.OrderRepository;
import com.restaurant.backend.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Xu ly nghiep vu don hang
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Set<String> STATUSES = Set.of("OPEN", "SERVING", "PAID", "CANCELLED");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // Them don hang
    @Override
    public Order create(Order order) {
        order.setId(null);
        return orderRepository.save(order);
    }

    // Lay danh sach don hang
    @Override
    @Transactional(readOnly = true)
    public List<Order> getAll(Integer customerId, String status) {
        List<Order> orders = customerId != null
                ? orderRepository.findByCustomerIdOrderByIdDesc(customerId)
                : orderRepository.findAll();
        if (status != null && !status.isBlank()) {
            orders = orders.stream().filter(o -> status.equals(o.getStatus())).toList();
        }
        return orders;
    }

    // Lay don hang theo ID
    @Override
    @Transactional(readOnly = true)
    public Order getById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Khong tim thay don hang co ID: " + id));
    }

    // Cap nhat don hang
    @Override
    public Order update(Integer id, Order order) {
        Order existingOrder = getById(id);
        existingOrder.setCustomerId(order.getCustomerId());
        existingOrder.setTableId(order.getTableId());
        existingOrder.setEmployeeId(order.getEmployeeId());
        existingOrder.setCreatedAt(order.getCreatedAt());
        existingOrder.setStatus(order.getStatus());
        return orderRepository.save(existingOrder);
    }

    // Doi trang thai don hang
    @Override
    public Order updateStatus(Integer id, String status) {
        if (status == null || !STATUSES.contains(status)) {
            throw new BadRequestException("Trạng thái đơn hàng không hợp lệ");
        }
        Order existingOrder = getById(id);
        existingOrder.setStatus(status);
        return orderRepository.save(existingOrder);
    }

    // Xoa don hang
    @Override
    public void delete(Integer id) {
        Order existingOrder = getById(id);
        orderRepository.delete(existingOrder);
    }

    // Tinh tong tien va so mon cua nhieu don bang 1 cau truy van
    @Override
    @Transactional(readOnly = true)
    public Map<Integer, OrderTotals> totals(Collection<Integer> orderIds) {
        Map<Integer, OrderTotals> result = new HashMap<>();
        if (orderIds == null || orderIds.isEmpty()) {
            return result;
        }
        for (Object[] row : orderItemRepository.aggregateByOrderIds(orderIds)) {
            Integer orderId = ((Number) row[0]).intValue();
            BigDecimal total = row[1] == null ? BigDecimal.ZERO : new BigDecimal(row[1].toString());
            long qty = row[2] == null ? 0 : ((Number) row[2]).longValue();
            result.put(orderId, new OrderTotals(total, qty));
        }
        return result;
    }
}
