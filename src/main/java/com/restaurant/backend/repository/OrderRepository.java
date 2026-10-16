package com.restaurant.backend.repository;

import com.restaurant.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repository thao tac voi bang orders
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Don hang cua 1 khach (moi nhat truoc)
    List<Order> findByCustomerIdOrderByIdDesc(Integer customerId);
}
