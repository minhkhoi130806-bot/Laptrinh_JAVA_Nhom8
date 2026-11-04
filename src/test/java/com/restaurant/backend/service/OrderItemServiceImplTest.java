package com.restaurant.backend.service;

import com.restaurant.backend.entity.Ingredient;
import com.restaurant.backend.entity.Menuitem;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemId;
import com.restaurant.backend.entity.Recipe;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.repository.MenuItemRepository;
import com.restaurant.backend.repository.OrderItemRepository;
import com.restaurant.backend.repository.OrderRepository;
import com.restaurant.backend.repository.RecipeRepository;
import com.restaurant.backend.service.Impl.OrderItemServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplTest {
    @Mock OrderItemRepository lines;
    @Mock OrderRepository orders;
    @Mock MenuItemRepository menu;
    @Mock RecipeRepository recipes;
    @Mock EntityManager entityManager;
    @InjectMocks OrderItemServiceImpl service;

    @Test
    void addingDifferentDishChecksCombinedNeedForSharedIngredient() {
        openOrder();
        availableDish(2);
        OrderItem old = line(1, 1, "100000");
        Ingredient beef = ingredient("1.0");
        when(lines.findByOrderIdForUpdate(9)).thenReturn(List.of(old));
        when(recipes.findByMenuItem(1L)).thenReturn(List.of(recipe(beef, "0.6")));
        when(recipes.findByMenuItem(2L)).thenReturn(List.of(recipe(beef, "0.6")));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.create(line(2, 1, "1")));

        assertTrue(error.getMessage().contains("tổng cần: 1.2"));
        assertEquals(1, old.getQuantity());
        verify(lines, never()).save(any());
    }

    @Test
    void addingSameDishChecksTotalQuantityAndDoesNotMutateOnFailure() {
        openOrder();
        availableDish(1);
        OrderItem old = line(1, 2, "80000");
        when(lines.findById(old.getId())).thenReturn(Optional.of(old));
        when(lines.findByOrderIdForUpdate(9)).thenReturn(List.of(old));
        when(recipes.findByMenuItem(1L)).thenReturn(List.of(recipe(ingredient("1"), "0.4")));

        assertThrows(BadRequestException.class, () -> service.create(line(1, 1, "1")));
        assertEquals(2, old.getQuantity());
        assertEquals(new BigDecimal("80000"), old.getUnitPrice());
        verify(lines, never()).save(any());
    }

    @Test
    void newLineAlwaysUsesServerMenuPrice() {
        openOrder();
        availableDish(1);
        OrderItem input = line(1, 2, "1");
        when(lines.save(input)).thenReturn(input);

        OrderItem result = service.create(input);

        assertEquals(new BigDecimal("100000"), result.getUnitPrice());
        verify(orders).findByIdForUpdate(9);
    }

    @Test
    void addingExistingDishPreservesPriceAgreedBeforeMenuPriceChange() {
        openOrder();
        availableDish(1);
        OrderItem old = line(1, 2, "80000");
        when(lines.findById(old.getId())).thenReturn(Optional.of(old));
        when(lines.findByOrderIdForUpdate(9)).thenReturn(List.of(old));
        when(lines.save(old)).thenReturn(old);

        OrderItem result = service.create(line(1, 1, "1"));

        assertEquals(3, result.getQuantity());
        assertEquals(new BigDecimal("80000"), result.getUnitPrice());
    }

    @Test
    void increasingQuantityChecksOtherDishesBeforeSaving() {
        openOrder();
        availableDish(1);
        OrderItem old = line(1, 1, "100000");
        Ingredient beef = ingredient("1.0");
        when(lines.findById(old.getId())).thenReturn(Optional.of(old));
        when(lines.findByOrderIdForUpdate(9)).thenReturn(List.of(old, line(2, 1, "100000")));
        when(recipes.findByMenuItem(1L)).thenReturn(List.of(recipe(beef, "0.4")));
        when(recipes.findByMenuItem(2L)).thenReturn(List.of(recipe(beef, "0.4")));

        assertThrows(BadRequestException.class, () -> service.update(old.getId(), line(1, 2, "1")));
        assertEquals(1, old.getQuantity());
        verify(lines, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PAID", "CANCELLED", "UNKNOWN"})
    void endedOrInvalidOrderRejectsEveryDishMutation(String status) {
        Order order = new Order();
        order.setStatus(status);
        when(orders.findByIdForUpdate(9)).thenReturn(Optional.of(order));
        OrderItem input = line(1, 1, "100000");

        assertThrows(BadRequestException.class, () -> service.create(input));
        assertThrows(BadRequestException.class, () -> service.update(input.getId(), input));
        assertThrows(BadRequestException.class, () -> service.delete(input.getId()));

        verifyNoInteractions(lines, menu, recipes);
    }

    @Test
    void reducingQuantityAndEditingNotesCannotChangeSnapshotPrice() {
        openOrder();
        OrderItem old = line(1, 2, "80000");
        when(lines.findById(old.getId())).thenReturn(Optional.of(old));
        when(lines.save(old)).thenReturn(old);
        OrderItem input = line(1, 1, "1");
        input.setNote("  Không cay  ");

        OrderItem result = service.update(old.getId(), input);

        assertEquals(1, result.getQuantity());
        assertEquals(new BigDecimal("80000"), result.getUnitPrice());
        assertEquals("Không cay", result.getNote());
        verifyNoInteractions(menu, recipes);
    }

    @Test
    void zeroQuantityRemovesOnlyFromOpenOrder() {
        openOrder();
        OrderItem old = line(1, 2, "80000");
        when(lines.findById(old.getId())).thenReturn(Optional.of(old));

        service.update(old.getId(), line(1, 0, "1"));

        verify(lines).delete(old);
        verify(lines, never()).save(any());
    }

    @Test
    void deletedDishCannotBeAddedEvenWhenMarkedAvailable() {
        openOrder();
        Menuitem dish = availableDish(1);
        dish.setDeleted(true);

        assertThrows(BadRequestException.class, () -> service.create(line(1, 1, "1")));
        verifyNoInteractions(lines, recipes);
    }

    @Test
    void invalidQuantityIsRejectedRatherThanSilentlyCreatingOnePortion() {
        openOrder();
        availableDish(1);
        assertThrows(BadRequestException.class, () -> service.create(line(1, -3, "1")));
        verifyNoInteractions(lines, recipes);
    }

    private void openOrder() {
        Order order = new Order();
        order.setId(9);
        order.setStatus("OPEN");
        when(orders.findByIdForUpdate(9)).thenReturn(Optional.of(order));
    }

    private Menuitem availableDish(int id) {
        Menuitem dish = new Menuitem();
        dish.setId((long) id);
        dish.setNameVi("Món " + id);
        dish.setAvailable(true);
        dish.setPrice(new BigDecimal("100000"));
        when(menu.findById((long) id)).thenReturn(Optional.of(dish));
        return dish;
    }

    private static OrderItem line(int menuItemId, int quantity, String price) {
        OrderItem item = new OrderItem();
        item.setId(new OrderItemId(9, menuItemId));
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(price));
        return item;
    }

    private static Ingredient ingredient(String stock) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(5L);
        ingredient.setName("Thịt bò");
        ingredient.setUnit("kg");
        ingredient.setStockQty(new BigDecimal(stock));
        return ingredient;
    }

    private static Recipe recipe(Ingredient ingredient, String quantity) {
        Recipe recipe = new Recipe();
        recipe.setIngredient(ingredient);
        recipe.setQuantityNeeded(new BigDecimal(quantity));
        return recipe;
    }
}
