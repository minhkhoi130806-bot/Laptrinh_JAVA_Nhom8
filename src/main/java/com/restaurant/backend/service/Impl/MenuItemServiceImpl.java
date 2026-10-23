package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.ListScope;
import com.restaurant.backend.dto.MenuItemRequest;
import com.restaurant.backend.dto.MenuItemResponse;
import com.restaurant.backend.dto.MenuItemResponse.BestSellerResponse;
import com.restaurant.backend.dto.MenuItemResponse.MenuItemStockStatus;
import com.restaurant.backend.dto.RecipeLineRequest;
import com.restaurant.backend.dto.RecipeLineResponse;
import com.restaurant.backend.entity.Category;
import com.restaurant.backend.entity.Ingredient;
import com.restaurant.backend.entity.Menuitem;
import com.restaurant.backend.entity.Recipe;
import com.restaurant.backend.entity.RecipeId;
import com.restaurant.backend.entity.Spicelevel;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.mapper.MenuItemMapper;
import com.restaurant.backend.repository.CategoryRepository;
import com.restaurant.backend.repository.IngredientRepository;
import com.restaurant.backend.repository.MenuItemRepository;
import com.restaurant.backend.repository.OrderItemRepository;
import com.restaurant.backend.repository.RecipeRepository;
import com.restaurant.backend.service.FileStorageService;
import com.restaurant.backend.service.MenuItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemMapper mapper;
    private final FileStorageService storageService;

    public MenuItemServiceImpl(MenuItemRepository menuItemRepository,
                               CategoryRepository categoryRepository,
                               IngredientRepository ingredientRepository,
                               RecipeRepository recipeRepository,
                               OrderItemRepository orderItemRepository,
                               MenuItemMapper mapper,
                               FileStorageService storageService) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.recipeRepository = recipeRepository;
        this.orderItemRepository = orderItemRepository;
        this.mapper = mapper;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public MenuItemResponse create(MenuItemRequest request) {
        validate(request);
        Menuitem item = new Menuitem();
        apply(item, request);
        return mapper.toResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAll(ListScope scope, Long categoryId) {
        ListScope effectiveScope = scope == null ? ListScope.ACTIVE : scope;
        return menuItemRepository.findAllWithCategory().stream()
                .filter(m -> matchesScope(effectiveScope, m.getDeleted()))
                .filter(m -> categoryId == null || Objects.equals(m.getCategory().getId(), categoryId))
                .map(mapper::toResponse)
                .toList();
    }

    private static boolean matchesScope(ListScope scope, Boolean deleted) {
        return switch (scope) {
            case ACTIVE -> !Boolean.TRUE.equals(deleted);
            case TRASH -> Boolean.TRUE.equals(deleted);
            case ALL -> true;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse getById(Long id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional
    public MenuItemResponse update(Long id, MenuItemRequest request) {
        validate(request);
        Menuitem item = find(id);
        String oldImage = item.getImageUrl();
        apply(item, request);
        Menuitem saved = menuItemRepository.save(item);

        if (oldImage != null && !oldImage.equals(saved.getImageUrl())) {
            storageService.deleteQuietly(oldImage);
        }
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MenuItemResponse setAvailable(Long id, boolean available) {
        Menuitem item = find(id);
        item.setAvailable(available);
        return mapper.toResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Menuitem item = find(id);
        if (Boolean.TRUE.equals(item.getDeleted())) return;
        item.setDeleted(true);
        item.setDeletedAt(LocalDateTime.now());
        item.setAvailable(false);
        menuItemRepository.save(item);
    }

    @Override
    @Transactional
    public MenuItemResponse restore(Long id) {
        Menuitem item = find(id);
        if (!Boolean.TRUE.equals(item.getDeleted())) return mapper.toResponse(item);
        item.setDeleted(false);
        item.setDeletedAt(null);
        return mapper.toResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional
    public void purge(Long id) {
        Menuitem item = find(id);
        if (!Boolean.TRUE.equals(item.getDeleted())) {
            throw new BadRequestException("Chỉ có thể xóa vĩnh viễn món đang nằm trong thùng rác");
        }
        recipeRepository.deleteByMenuItem(id);
        String img = item.getImageUrl();
        menuItemRepository.delete(item);
        menuItemRepository.flush();
        storageService.deleteQuietly(img);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeLineResponse> getRecipe(Long menuItemId) {
        find(menuItemId);
        return recipeRepository.findByMenuItem(menuItemId).stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public List<RecipeLineResponse> saveRecipe(Long menuItemId, List<RecipeLineRequest> lines) {
        Menuitem item = find(menuItemId);
        List<RecipeLineRequest> input = lines == null ? List.of() : lines.stream()
                .filter(Objects::nonNull)
                .filter(l -> l.ingredientId() != null)
                .toList();

        Set<Long> seen = new HashSet<>();
        for (RecipeLineRequest line : input) {
            if (!seen.add(line.ingredientId())) {
                throw new BadRequestException("Mỗi nguyên liệu chỉ xuất hiện 1 lần trong công thức");
            }
            if (line.quantityNeeded() == null || line.quantityNeeded().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Định lượng nguyên liệu phải lớn hơn 0");
            }
        }

        recipeRepository.deleteByMenuItem(menuItemId);
        for (RecipeLineRequest line : input) {
            Ingredient ingredient = ingredientRepository.findById(line.ingredientId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy nguyên liệu id = " + line.ingredientId()));
            Recipe recipe = new Recipe();
            recipe.setId(new RecipeId(menuItemId, ingredient.getId()));
            recipe.setMenuItem(item);
            recipe.setIngredient(ingredient);
            recipe.setQuantityNeeded(line.quantityNeeded());
            recipeRepository.save(recipe);
        }
        recipeRepository.flush();
        return recipeRepository.findByMenuItem(menuItemId).stream().map(mapper::toResponse).toList();
    }

    // ---------- Tuần 4: Báo cáo & Đồng bộ trạng thái hết hàng ----------

    @Override
    @Transactional(readOnly = true)
    public List<BestSellerResponse> getBestSellers() {
        List<Object[]> rows = orderItemRepository.findBestSellingMenuItems();
        Map<Long, Menuitem> itemMap = menuItemRepository.findAllWithCategory().stream()
                .collect(Collectors.toMap(Menuitem::getId, m -> m));

        return rows.stream().map(row -> {
            Long menuItemId = ((Number) row[0]).longValue();
            Long qty = ((Number) row[1]).longValue();
            BigDecimal rev = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            Menuitem item = itemMap.get(menuItemId);
            String nameVi = item != null ? item.getNameVi() : "Món #" + menuItemId;
            String nameKo = item != null ? item.getNameKo() : null;
            BigDecimal price = item != null ? item.getPrice() : BigDecimal.ZERO;
            String catName = (item != null && item.getCategory() != null) ? item.getCategory().getName() : "—";
            return new BestSellerResponse(menuItemId, nameVi, nameKo, price, catName, qty, rev);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemStockStatus> getStockStatus() {
        List<Menuitem> items = menuItemRepository.findAllWithCategory();
        List<MenuItemStockStatus> results = new ArrayList<>();

        for (Menuitem item : items) {
            if (Boolean.TRUE.equals(item.getDeleted())) continue;
            List<Recipe> recipes = recipeRepository.findByMenuItem(item.getId());
            List<String> missing = new ArrayList<>();
            boolean enough = true;

            for (Recipe r : recipes) {
                Ingredient ing = r.getIngredient();
                BigDecimal stock = ing.getStockQty() != null ? ing.getStockQty() : BigDecimal.ZERO;
                BigDecimal needed = r.getQuantityNeeded() != null ? r.getQuantityNeeded() : BigDecimal.ZERO;
                if (stock.compareTo(needed) < 0) {
                    enough = false;
                    missing.add(ing.getName() + " (còn " + stock + " / cần " + needed + " " + ing.getUnit() + ")");
                }
            }

            results.add(new MenuItemStockStatus(
                    item.getId(),
                    item.getNameVi(),
                    item.getNameKo(),
                    item.getAvailable(),
                    enough,
                    missing
            ));
        }
        return results;
    }

    @Override
    @Transactional
    public int syncOutOfStockStatus() {
        List<Menuitem> items = menuItemRepository.findAllWithCategory();
        int updatedCount = 0;

        for (Menuitem item : items) {
            if (Boolean.TRUE.equals(item.getDeleted())) continue;
            List<Recipe> recipes = recipeRepository.findByMenuItem(item.getId());
            if (recipes.isEmpty()) continue; // Món chưa có công thức thì không tự động đổi

            boolean enough = true;
            for (Recipe r : recipes) {
                Ingredient ing = r.getIngredient();
                BigDecimal stock = ing.getStockQty() != null ? ing.getStockQty() : BigDecimal.ZERO;
                BigDecimal needed = r.getQuantityNeeded() != null ? r.getQuantityNeeded() : BigDecimal.ZERO;
                if (stock.compareTo(needed) < 0) {
                    enough = false;
                    break;
                }
            }

            // Nếu thiếu nguyên liệu mà món đang bật bán -> chuyển sang tạm hết hàng
            if (!enough && Boolean.TRUE.equals(item.getAvailable())) {
                item.setAvailable(false);
                menuItemRepository.save(item);
                updatedCount++;
            }
            // Nếu đủ nguyên liệu mà món đang tắt bán -> tự động mở lại
            else if (enough && Boolean.FALSE.equals(item.getAvailable())) {
                item.setAvailable(true);
                menuItemRepository.save(item);
                updatedCount++;
            }
        }
        return updatedCount;
    }

    // ---------- helpers ----------

    private Menuitem find(Long id) {
        return menuItemRepository.findWithCategoryById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy món ăn id = " + id));
    }

    private void apply(Menuitem item, MenuItemRequest r) {
        Category category = categoryRepository.findById(r.categoryId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục id = " + r.categoryId()));
        item.setCategory(category);
        item.setNameVi(r.nameVi().trim());
        item.setNameKo(blankToNull(r.nameKo()));
        item.setDescription(blankToNull(r.description()));
        item.setPrice(r.price());
        item.setImageUrl(blankToNull(r.imageUrl()));
        item.setSpiceLevel(r.spiceLevel() != null ? r.spiceLevel() : Spicelevel.NONE);
        item.setAvailable(r.available() != null ? r.available() : Boolean.TRUE);
    }

    private void validate(MenuItemRequest r) {
        if (r == null || r.nameVi() == null || r.nameVi().isBlank()) {
            throw new BadRequestException("Tên món (tiếng Việt) không được để trống");
        }
        if (r.categoryId() == null) {
            throw new BadRequestException("Vui lòng chọn danh mục");
        }
        if (r.price() == null || r.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Giá món không hợp lệ");
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
