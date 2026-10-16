package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.IngredientResponse;
import com.restaurant.backend.entity.Ingredient;
import org.springframework.stereotype.Component;

@Component
public class IngredientMapper {

    public IngredientResponse toResponse(Ingredient i) {
        boolean low = i.getStockQty() != null && i.getMinStock() != null
                && i.getStockQty().compareTo(i.getMinStock()) <= 0;
        return new IngredientResponse(i.getId(), i.getName(), i.getUnit(), i.getStockQty(), i.getMinStock(), low);
    }
}
