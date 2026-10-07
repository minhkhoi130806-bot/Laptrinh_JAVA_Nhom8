package com.restaurant.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

// Món ăn trong thực đơn
@Entity
@Table(name = "menu_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Menuitem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // N món - 1 nhóm (phía "nhiều" của quan hệ)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Category category;

    // Tên tiếng Việt
    @Column(name = "name_vi", nullable = false, length = 150)
    private String nameVi;

    // Tên tiếng Hàn
    @Column(name = "name_ko", length = 150)
    private String nameKo;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    // Đường dẫn ảnh, ví dụ /images/menu/bibimbap.jpg
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "spice_level", nullable = false, length = 20)
    private Spicelevel spiceLevel = Spicelevel.NONE;

    // true = còn bán, false = tạm ngưng / hết hàng
    @Column(nullable = false)
    private Boolean available = true;

}