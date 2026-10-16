package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.IngredientRequest;
import com.restaurant.backend.dto.IngredientResponse;
import com.restaurant.backend.entity.Ingredient;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.mapper.IngredientMapper;
import com.restaurant.backend.repository.IngredientRepository;
import com.restaurant.backend.service.IngredientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper mapper;

    public IngredientServiceImpl(IngredientRepository ingredientRepository, IngredientMapper mapper) {
        this.ingredientRepository = ingredientRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public IngredientResponse create(IngredientRequest request) {
        validate(request);
        String name = request.name().trim();
        if (ingredientRepository.existsByName(name)) {
            throw new BadRequestException("Tên nguyên liệu đã tồn tại");
        }
        Ingredient ingredient = new Ingredient();
        apply(ingredient, request);
        return mapper.toResponse(ingredientRepository.save(ingredient));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientResponse> getAll() {
        return ingredientRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public IngredientResponse getById(Long id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional
    public IngredientResponse update(Long id, IngredientRequest request) {
        validate(request);
        Ingredient existing = find(id);
        if (ingredientRepository.existsByNameAndIdNot(request.name().trim(), id)) {
            throw new BadRequestException("Tên nguyên liệu đã tồn tại");
        }
        apply(existing, request);
        return mapper.toResponse(ingredientRepository.save(existing));
    }

    // Nguyên liệu đang nằm trong công thức / phiếu nhập sẽ bị DB chặn -> GlobalExceptionHandler trả 409
    @Override
    @Transactional
    public void delete(Long id) {
        ingredientRepository.delete(find(id));
        ingredientRepository.flush();
    }

    private Ingredient find(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nguyên liệu id = " + id));
    }

    private void apply(Ingredient i, IngredientRequest r) {
        i.setName(r.name().trim());
        i.setUnit(r.unit().trim());
        i.setStockQty(r.stockQty() != null ? r.stockQty() : BigDecimal.ZERO);
        i.setMinStock(r.minStock() != null ? r.minStock() : BigDecimal.ZERO);
    }

    private void validate(IngredientRequest r) {
        if (r == null || r.name() == null || r.name().isBlank()) {
            throw new BadRequestException("Tên nguyên liệu không được để trống");
        }
        if (r.unit() == null || r.unit().isBlank()) {
            throw new BadRequestException("Đơn vị tính không được để trống");
        }
        if ((r.stockQty() != null && r.stockQty().compareTo(BigDecimal.ZERO) < 0)
                || (r.minStock() != null && r.minStock().compareTo(BigDecimal.ZERO) < 0)) {
            throw new BadRequestException("Số lượng tồn kho không được âm");
        }
    }
}
