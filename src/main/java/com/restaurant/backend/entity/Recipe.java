package com.restaurant.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

// Công thức món: 1 món cần bao nhiêu nguyên liệu nào cho 1 phần.
// Dùng để trừ kho khi thanh toán (tuần 4).
@Entity
@Table(name = "recipe")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {

    // Khóa chính kép (menu_item_id + ingredient_id)
    @EmbeddedId
    private RecipeId id = new RecipeId();

    // N công thức - 1 món. @MapsId lấy giá trị cho phần menuItemId của khóa kép
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("menuItemId")
    @JoinColumn(name = "menu_item_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Menuitem menuItem;

    // N công thức - 1 nguyên liệu
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("ingredientId")
    @JoinColumn(name = "ingredient_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Ingredient ingredient;

    // Lượng nguyên liệu cần cho 1 phần món (cùng đơn vị với Ingredient.unit)
    @Column(name = "quantity_needed", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityNeeded;
}
