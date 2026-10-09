package com.restaurant.backend.service.Impl;

import com.restaurant.backend.entity.Order;
import com.restaurant.backend.repository.OrderRepository;
import com.restaurant.backend.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Xu ly nghiep vu don hang
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Them don hang
    @Override
    public Order create(Order order) {
        order.setId(null);
        return orderRepository.save(order);
    }

    // Lay danh sach don hang
    @Override
    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    // Lay don hang theo ID
    @Override
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

    // Xoa don hang
    @Override
    public void delete(Integer id) {
        Order existingOrder = getById(id);
        orderRepository.delete(existingOrder);
    }
}
