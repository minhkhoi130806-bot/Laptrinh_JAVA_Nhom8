package com.restaurant.backend.service;

import com.restaurant.backend.entity.Ingredient;
import com.restaurant.backend.entity.Menuitem;
import com.restaurant.backend.entity.Recipe;
import com.restaurant.backend.mapper.MenuItemMapper;
import com.restaurant.backend.repository.CategoryRepository;
import com.restaurant.backend.repository.IngredientRepository;
import com.restaurant.backend.repository.MenuItemRepository;
import com.restaurant.backend.repository.OrderItemRepository;
import com.restaurant.backend.repository.RecipeRepository;
import com.restaurant.backend.service.Impl.MenuItemServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MenuItemStockSyncTest {
    @Test
    void syncDisablesShortDishButNeverReopensManuallyPausedDish() {
        MenuItemRepository menu = mock(MenuItemRepository.class);
        RecipeRepository recipes = mock(RecipeRepository.class);
        MenuItemServiceImpl service = new MenuItemServiceImpl(menu, mock(CategoryRepository.class),
                mock(IngredientRepository.class), recipes, mock(OrderItemRepository.class),
                mock(MenuItemMapper.class), mock(FileStorageService.class));
        Menuitem shortDish = dish(1, true);
        Menuitem pausedDish = dish(2, false);
        Menuitem recipeMissingDish = dish(3, true);
        when(menu.findAllWithCategory()).thenReturn(List.of(shortDish, pausedDish, recipeMissingDish));
        when(recipes.findByMenuItem(1L)).thenReturn(List.of(recipe("0.1", "0.2")));
        when(recipes.findByMenuItem(2L)).thenReturn(List.of(recipe("2", "0.2")));

        assertEquals(1, service.syncOutOfStockStatus());

        assertFalse(shortDish.getAvailable());
        assertFalse(pausedDish.getAvailable());
        assertTrue(recipeMissingDish.getAvailable());
        verify(menu).save(shortDish);
        verify(menu, never()).save(pausedDish);
        verify(menu, never()).save(recipeMissingDish);
    }

    private static Menuitem dish(long id, boolean available) {
        Menuitem item = new Menuitem();
        item.setId(id);
        item.setAvailable(available);
        return item;
    }

    private static Recipe recipe(String stock, String needed) {
        Ingredient ingredient = new Ingredient();
        ingredient.setStockQty(new BigDecimal(stock));
        Recipe recipe = new Recipe();
        recipe.setIngredient(ingredient);
        recipe.setQuantityNeeded(new BigDecimal(needed));
        return recipe;
    }
}
