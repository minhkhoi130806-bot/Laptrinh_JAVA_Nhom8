package com.restaurant.backend.service;

import com.restaurant.backend.dto.OrderTotals;
import com.restaurant.backend.entity.Order;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// Cac chuc nang xu ly don hang
public interface OrderService {

    // Them don hang
    Order create(Order order);

    // Lay danh sach don hang (loc theo khach / trang thai neu co)
    List<Order> getAll(Integer customerId, String status);

    // Lay don hang theo ID
    Order getById(Integer id);

    // Cap nhat don hang
    Order update(Integer id, Order order);

    // Doi trang thai don hang
    Order updateStatus(Integer id, String status);

    // Xoa don hang
    void delete(Integer id);

    // Tong tien / so mon cua cac don
    Map<Integer, OrderTotals> totals(Collection<Integer> orderIds);
}
