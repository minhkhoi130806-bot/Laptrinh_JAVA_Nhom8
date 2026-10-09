
package com.restaurant.backend.repository;

import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repository thao tac voi bang order_item
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemId> {
}