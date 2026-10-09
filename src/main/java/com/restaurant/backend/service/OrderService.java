
package com.restaurant.backend.service;

import com.restaurant.backend.entity.Order;

import java.util.List;

// Cac chuc nang xu ly don hang
public interface OrderService {

    Order create(Order order);

    List<Order> getAll();

    Order getById(Integer id);

    Order update(Integer id, Order order);

    void delete(Integer id);
}