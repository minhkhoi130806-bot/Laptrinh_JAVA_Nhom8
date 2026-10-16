package com.restaurant.backend.controller;

import com.restaurant.backend.dto.IngredientRequest;
import com.restaurant.backend.dto.IngredientResponse;
import com.restaurant.backend.service.IngredientService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientResponse create(@RequestBody IngredientRequest request) {
        return ingredientService.create(request);
    }

    @GetMapping
    public List<IngredientResponse> getAll() {
        return ingredientService.getAll();
    }

    @GetMapping("/{id}")
    public IngredientResponse getById(@PathVariable Long id) {
        return ingredientService.getById(id);
    }

    @PutMapping("/{id}")
    public IngredientResponse update(@PathVariable Long id, @RequestBody IngredientRequest request) {
        return ingredientService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ingredientService.delete(id);
    }
}
