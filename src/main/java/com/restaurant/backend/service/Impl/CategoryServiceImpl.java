package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.CategoryRequest;
import com.restaurant.backend.dto.CategoryResponse;
import com.restaurant.backend.entity.Category;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.mapper.CategoryMapper;
import com.restaurant.backend.repository.CategoryRepository;
import com.restaurant.backend.repository.MenuItemRepository;
import com.restaurant.backend.service.CategoryService;
import com.restaurant.backend.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final CategoryMapper categoryMapper;
    private final FileStorageService storageService;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               MenuItemRepository menuItemRepository,
                               CategoryMapper categoryMapper,
                               FileStorageService storageService) {
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.categoryMapper = categoryMapper;
        this.storageService = storageService;
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        validate(request);
        String name = request.name().trim();
        if (categoryRepository.existsByName(name)) {
            throw new BadRequestException("Tên danh mục đã tồn tại");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(blankToNull(request.description()));
        category.setImageUrl(blankToNull(request.imageUrl()));
        return categoryMapper.toResponse(categoryRepository.save(category), 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : menuItemRepository.countActiveByCategory()) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return categoryRepository.findAll().stream()
                .map(c -> categoryMapper.toResponse(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = find(id);
        return categoryMapper.toResponse(category, countItems(id));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        validate(request);
        Category existing = find(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new BadRequestException("Tên danh mục đã tồn tại");
        }
        String oldImage = existing.getImageUrl();
        String newImage = blankToNull(request.imageUrl());
        existing.setName(name);
        existing.setDescription(blankToNull(request.description()));
        existing.setImageUrl(newImage);
        Category saved = categoryRepository.save(existing);
        // Đổi / gỡ ảnh thì dọn file ảnh cũ
        if (oldImage != null && !Objects.equals(oldImage, newImage)) {
            storageService.deleteQuietly(oldImage);
        }
        return categoryMapper.toResponse(saved, countItems(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category existing = find(id);
        long total = menuItemRepository.countByCategoryId(id);
        if (total > 0) {
            throw new BadRequestException("Danh mục đang có " + total
                    + " món ăn (kể cả trong thùng rác). Hãy chuyển hoặc xóa vĩnh viễn các món đó trước");
        }
        categoryRepository.delete(existing);
        storageService.deleteQuietly(existing.getImageUrl());
    }

    private Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục id = " + id));
    }

    private long countItems(Long id) {
        return menuItemRepository.countByCategoryIdAndDeleted(id, false);
    }

    private void validate(CategoryRequest r) {
        if (r == null || r.name() == null || r.name().isBlank()) {
            throw new BadRequestException("Tên danh mục không được để trống");
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
