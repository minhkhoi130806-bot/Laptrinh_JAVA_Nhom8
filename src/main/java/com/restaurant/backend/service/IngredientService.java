package com.restaurant.backend.service;

import com.restaurant.backend.dto.IngredientRequest;
import com.restaurant.backend.dto.IngredientResponse;

import java.util.List;

public interface IngredientService {
    IngredientResponse create(IngredientRequest request);
    List<IngredientResponse> getAll();
    IngredientResponse getById(Long id);
    IngredientResponse update(Long id, IngredientRequest request);
    void delete(Long id);
}
