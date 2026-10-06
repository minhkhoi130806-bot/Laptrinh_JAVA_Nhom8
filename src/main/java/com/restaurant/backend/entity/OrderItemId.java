package com.restaurant.backend.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class OrderItemId implements Serializable {

    // Ma don hang
    private Integer orderId;

    // Ma mon an
    private Integer menuItemId;

    public OrderItemId() {
    }

    public OrderItemId(Integer orderId, Integer menuItemId) {
        this.orderId = orderId;
        this.menuItemId = menuItemId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(Integer menuItemId) {
        this.menuItemId = menuItemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItemId)) return false;

        OrderItemId that = (OrderItemId) o;

        return Objects.equals(orderId, that.orderId)
                && Objects.equals(menuItemId, that.menuItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, menuItemId);
    }
}