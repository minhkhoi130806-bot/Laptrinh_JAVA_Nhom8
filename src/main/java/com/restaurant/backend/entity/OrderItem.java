package com.restaurant.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
public class OrderItem {

    // Khoa chinh ghep gom order_id va menu_item_id
    @EmbeddedId
    private OrderItemId id;

    // So luong mon
    private Integer quantity;

    // Don gia tai thoi diem dat mon
    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    // Ghi chu cho mon an
    private String note;

    public OrderItem() {
    }

    public OrderItemId getId() {
        return id;
    }

    public void setId(OrderItemId id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}