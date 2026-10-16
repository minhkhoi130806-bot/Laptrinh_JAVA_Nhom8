package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.CategoryResponse;
import com.restaurant.backend.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category c, long itemCount) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getImageUrl(), itemCount);
    }
}
