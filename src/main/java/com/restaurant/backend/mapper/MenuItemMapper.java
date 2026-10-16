package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.MenuItemResponse;
import com.restaurant.backend.dto.RecipeLineResponse;
import com.restaurant.backend.entity.Menuitem;
import com.restaurant.backend.entity.Recipe;
import org.springframework.stereotype.Component;

// Gọi trong service (có transaction) vì category / ingredient là LAZY
@Component
public class MenuItemMapper {

    public MenuItemResponse toResponse(Menuitem m) {
        return new MenuItemResponse(
                m.getId(),
                m.getCategory().getId(),
                m.getCategory().getName(),
                m.getNameVi(),
                m.getNameKo(),
                m.getDescription(),
                m.getPrice(),
                m.getImageUrl(),
                m.getSpiceLevel(),
                m.getAvailable(),
                m.getDeleted(),
                m.getDeletedAt()
        );
    }

    public RecipeLineResponse toResponse(Recipe r) {
        return new RecipeLineResponse(
                r.getIngredient().getId(),
                r.getIngredient().getName(),
                r.getIngredient().getUnit(),
                r.getQuantityNeeded()
        );
    }
}
