package com.restaurant.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Nguyên liệu trong kho
@Entity
@Table(name = "ingredient")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    // Đơn vị tính: kg, lít, gói...
    @Column(nullable = false, length = 20)
    private String unit;

    // Số lượng tồn hiện tại
    @Column(name = "stock_qty", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockQty = BigDecimal.ZERO;

    // Mức tồn tối thiểu, dưới mức này thì cảnh báo
    @Column(name = "min_stock", nullable = false, precision = 12, scale = 3)
    private BigDecimal minStock = BigDecimal.ZERO;
}