package com.restaurant.backend.service.Impl;

import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemId;
import com.restaurant.backend.repository.OrderItemRepository;
import com.restaurant.backend.service.OrderItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Xu ly nghiep vu chi tiet don hang
@Service
@Transactional
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItemServiceImpl(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    // Them chi tiet don hang
    @Override
    public OrderItem create(OrderItem orderItem) {
        return orderItemRepository.save(orderItem);
    }

    // Lay danh sach chi tiet don hang
    @Override
    public List<OrderItem> getAll() {
        return orderItemRepository.findAll();
    }

    // Tim chi tiet theo khoa chinh kep
    @Override
    public OrderItem getById(OrderItemId id) {
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Khong tim thay chi tiet don hang"));
    }

    // Cap nhat chi tiet don hang
    @Override
    public OrderItem update(OrderItemId id, OrderItem orderItem) {
        OrderItem existingItem = getById(id);

        existingItem.setQuantity(orderItem.getQuantity());
        existingItem.setUnitPrice(orderItem.getUnitPrice());
        existingItem.setNote(orderItem.getNote());

        return orderItemRepository.save(existingItem);
    }

    // Xoa chi tiet don hang
    @Override
    public void delete(OrderItemId id) {
        OrderItem existingItem = getById(id);
        orderItemRepository.delete(existingItem);
    }
}