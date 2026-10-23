package com.restaurant.backend.controller;

import com.restaurant.backend.dto.ListScope;
import com.restaurant.backend.dto.MenuItemRequest;
import com.restaurant.backend.dto.MenuItemResponse;
import com.restaurant.backend.dto.MenuItemResponse.BestSellerResponse;
import com.restaurant.backend.dto.MenuItemResponse.MenuItemStockStatus;
import com.restaurant.backend.dto.RecipeLineRequest;
import com.restaurant.backend.dto.RecipeLineResponse;
import com.restaurant.backend.service.MenuItemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menu-items")
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse create(@RequestBody MenuItemRequest request) {
        return menuItemService.create(request);
    }

    // GET /api/menu-items  |  ?categoryId=1  |  ?scope=TRASH  |  ?scope=ALL
    @GetMapping
    public List<MenuItemResponse> getAll(@RequestParam(defaultValue = "ACTIVE") ListScope scope,
                                         @RequestParam(required = false) Long categoryId) {
        return menuItemService.getAll(scope, categoryId);
    }

    // Tuần 4: Top món bán chạy
    @GetMapping("/best-sellers")
    public List<BestSellerResponse> getBestSellers() {
        return menuItemService.getBestSellers();
    }

    // Tuần 4: Kiểm tra trạng thái tồn kho của món theo công thức
    @GetMapping("/stock-status")
    public List<MenuItemStockStatus> getStockStatus() {
        return menuItemService.getStockStatus();
    }

    // Tuần 4: Tự động đánh dấu món hết hàng dựa theo nguyên liệu kho
    @PostMapping("/sync-out-of-stock")
    public Map<String, Object> syncOutOfStock() {
        int updated = menuItemService.syncOutOfStockStatus();
        return Map.of("updatedCount", updated, "message", "Đã đồng bộ trạng thái " + updated + " món ăn theo kho.");
    }

    @GetMapping("/{id}")
    public MenuItemResponse getById(@PathVariable Long id) {
        return menuItemService.getById(id);
    }

    @PutMapping("/{id}")
    public MenuItemResponse update(@PathVariable Long id, @RequestBody MenuItemRequest request) {
        return menuItemService.update(id, request);
    }

    // PATCH /api/menu-items/{id}/available?value=false  (bật/tắt bán nhanh)
    @PatchMapping("/{id}/available")
    public MenuItemResponse setAvailable(@PathVariable Long id, @RequestParam("value") boolean value) {
        return menuItemService.setAvailable(id, value);
    }

    // Chuyển vào thùng rác
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        menuItemService.delete(id);
    }

    // Khôi phục từ thùng rác
    @PostMapping("/{id}/restore")
    public MenuItemResponse restore(@PathVariable Long id) {
        return menuItemService.restore(id);
    }

    // Xóa vĩnh viễn (chỉ khi đang ở thùng rác)
    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purge(@PathVariable Long id) {
        menuItemService.purge(id);
    }

    // ----- Công thức món -----
    @GetMapping("/{id}/recipe")
    public List<RecipeLineResponse> getRecipe(@PathVariable Long id) {
        return menuItemService.getRecipe(id);
    }

    // Body: [{"ingredientId":1,"quantityNeeded":0.2}, ...]  (thay toàn bộ công thức)
    @PutMapping("/{id}/recipe")
    public List<RecipeLineResponse> saveRecipe(@PathVariable Long id, @RequestBody List<RecipeLineRequest> lines) {
        return menuItemService.saveRecipe(id, lines);
    }
}
