package com.restaurant.backend.service;

import com.restaurant.backend.dto.ListScope;
import com.restaurant.backend.dto.MenuItemRequest;
import com.restaurant.backend.dto.MenuItemResponse;
import com.restaurant.backend.dto.RecipeLineRequest;
import com.restaurant.backend.dto.RecipeLineResponse;

import java.util.List;

public interface MenuItemService {
    MenuItemResponse create(MenuItemRequest request);
    List<MenuItemResponse> getAll(ListScope scope, Long categoryId);
    MenuItemResponse getById(Long id);
    MenuItemResponse update(Long id, MenuItemRequest request);
    MenuItemResponse setAvailable(Long id, boolean available);
    void delete(Long id);                 // chuyển vào thùng rác
    MenuItemResponse restore(Long id);    // khôi phục từ thùng rác
    void purge(Long id);                  // xóa vĩnh viễn (kèm công thức và file ảnh)

    List<RecipeLineResponse> getRecipe(Long menuItemId);
    List<RecipeLineResponse> saveRecipe(Long menuItemId, List<RecipeLineRequest> lines);
}
