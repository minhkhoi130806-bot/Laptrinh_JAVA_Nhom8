package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.ListScope;
import com.restaurant.backend.dto.MenuItemRequest;
import com.restaurant.backend.dto.MenuItemResponse;
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
import com.restaurant.backend.repository.RecipeRepository;
import com.restaurant.backend.service.FileStorageService;
import com.restaurant.backend.service.MenuItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final MenuItemMapper mapper;
    private final FileStorageService storageService;

    public MenuItemServiceImpl(MenuItemRepository menuItemRepository,
                               CategoryRepository categoryRepository,
                               IngredientRepository ingredientRepository,
                               RecipeRepository recipeRepository,
                               MenuItemMapper mapper,
                               FileStorageService storageService) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.recipeRepository = recipeRepository;
        this.mapper = mapper;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public MenuItemResponse create(MenuItemRequest request) {
        validate(request);
        Menuitem item = new Menuitem();
        apply(item, request);
        item.setDeleted(false);
        item.setDeletedAt(null);
        return mapper.toResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getAll(ListScope scope, Long categoryId) {
        ListScope s = scope == null ? ListScope.ACTIVE : scope;
        return menuItemRepository.findAllWithCategory().stream()
                .filter(m -> switch (s) {
                    case ACTIVE -> !Boolean.TRUE.equals(m.getDeleted());
                    case TRASH -> Boolean.TRUE.equals(m.getDeleted());
                    case ALL -> true;
                })
                .filter(m -> categoryId == null || m.getCategory().getId().equals(categoryId))
                .map(mapper::toResponse)
                .toList();
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
        Menuitem existing = find(id);
        String oldImage = existing.getImageUrl();
        apply(existing, request);
        Menuitem saved = menuItemRepository.save(existing);
        // Đổi / gỡ ảnh thì dọn file ảnh cũ
        if (oldImage != null && !Objects.equals(oldImage, saved.getImageUrl())) {
            storageService.deleteQuietly(oldImage);
        }
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MenuItemResponse setAvailable(Long id, boolean available) {
        Menuitem existing = find(id);
        existing.setAvailable(available);
        return mapper.toResponse(menuItemRepository.save(existing));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Menuitem existing = find(id);
        existing.setDeleted(true);
        existing.setDeletedAt(LocalDateTime.now());
        existing.setAvailable(false); // món trong thùng rác không được bán
        menuItemRepository.save(existing);
    }

    @Override
    @Transactional
    public MenuItemResponse restore(Long id) {
        Menuitem existing = find(id);
        existing.setDeleted(false);
        existing.setDeletedAt(null);
        return mapper.toResponse(menuItemRepository.save(existing));
    }

    @Override
    @Transactional
    public void purge(Long id) {
        Menuitem existing = find(id);
        if (!Boolean.TRUE.equals(existing.getDeleted())) {
            throw new BadRequestException("Chỉ xóa vĩnh viễn được món đang trong thùng rác");
        }
        recipeRepository.deleteByMenuItem(id);
        menuItemRepository.delete(existing);
        storageService.deleteQuietly(existing.getImageUrl());
    }

    // ---------- Công thức ----------

    @Override
    @Transactional(readOnly = true)
    public List<RecipeLineResponse> getRecipe(Long menuItemId) {
        find(menuItemId);
        return recipeRepository.findByMenuItem(menuItemId).stream().map(mapper::toResponse).toList();
    }

    // Thay toàn bộ công thức của món bằng danh sách mới
    @Override
    @Transactional
    public List<RecipeLineResponse> saveRecipe(Long menuItemId, List<RecipeLineRequest> lines) {
        Menuitem item = find(menuItemId);
        List<RecipeLineRequest> input = lines == null ? List.of() : lines;

        Set<Long> seen = new HashSet<>();
        for (RecipeLineRequest line : input) {
            if (line.ingredientId() == null) {
                throw new BadRequestException("Thiếu nguyên liệu trong công thức");
            }
            if (line.quantityNeeded() == null || line.quantityNeeded().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Định lượng nguyên liệu phải lớn hơn 0");
            }
            if (!seen.add(line.ingredientId())) {
                throw new BadRequestException("Một nguyên liệu chỉ xuất hiện 1 lần trong công thức");
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
