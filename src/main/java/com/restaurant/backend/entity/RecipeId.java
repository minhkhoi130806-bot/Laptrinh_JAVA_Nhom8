package com.restaurant.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Khóa chính kép của bảng RECIPE: (menu_item_id, ingredient_id)
// @Data đã sinh equals/hashCode - bắt buộc phải có với @Embeddable
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeId implements Serializable {

    @Column(name = "menu_item_id")
    private Long menuItemId;

    @Column(name = "ingredient_id")
    private Long ingredientId;
}
