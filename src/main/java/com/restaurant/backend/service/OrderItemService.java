package com.restaurant.backend.service;

import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemId;

import java.util.List;

// Cac chuc nang xu ly chi tiet don hang
public interface OrderItemService {

    // Them chi tiet don hang
    OrderItem create(OrderItem orderItem);

    // Lay danh sach chi tiet don hang
    List<OrderItem> getAll();

    // Tim chi tiet theo ma don hang va ma mon an
    OrderItem getById(OrderItemId id);

    // Cap nhat chi tiet don hang
    OrderItem update(OrderItemId id, OrderItem orderItem);

    // Xoa chi tiet don hang
    void delete(OrderItemId id);
}