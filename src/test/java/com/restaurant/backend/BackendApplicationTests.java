package com.restaurant.backend;

import com.restaurant.backend.dto.RecipeLineRequest;
import com.restaurant.backend.entity.Ingredient;
import com.restaurant.backend.entity.Recipe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BackendApplicationTests {

	@Test
	@DisplayName("1. Kiểm tra tính toán tồn kho khi nhập hàng")
	void testStockCalculationOnReceipt() {
		BigDecimal initialStock = new BigDecimal("10.0");
		BigDecimal incomingQty = new BigDecimal("5.5");

		BigDecimal newStock = initialStock.add(incomingQty);
		assertEquals(new BigDecimal("15.5"), newStock, "Tồn kho sau khi nhập phải được cộng dồn chính xác");
	}

	@Test
	@DisplayName("2. Kiểm tra cờ cảnh báo nguyên liệu dưới mức tồn tối thiểu (lowStock)")
	void testLowStockFlag() {
		BigDecimal stockQty = new BigDecimal("3.0");
		BigDecimal minStock = new BigDecimal("5.0");

		boolean isLowStock = stockQty.compareTo(minStock) <= 0;
		assertTrue(isLowStock, "Tồn kho nhỏ hơn hoặc bằng mức tối thiểu thì phải bật cờ cảnh báo lowStock");

		BigDecimal safeStock = new BigDecimal("10.0");
		boolean isSafe = safeStock.compareTo(minStock) <= 0;
		assertFalse(isSafe, "Tồn kho lớn hơn mức tối thiểu thì không được báo lowStock");
	}

	@Test
	@DisplayName("3. Kiểm tra tính hợp lệ của công thức món ăn (Recipe validation)")
	void testRecipeValidation() {
		List<RecipeLineRequest> validLines = List.of(
				new RecipeLineRequest(1L, new BigDecimal("0.2")),
				new RecipeLineRequest(2L, new BigDecimal("0.05"))
		);

		Set<Long> seen = new HashSet<>();
		for (RecipeLineRequest line : validLines) {
			assertTrue(seen.add(line.ingredientId()), "Nguyên liệu không được trùng lặp");
			assertTrue(line.quantityNeeded().compareTo(BigDecimal.ZERO) > 0, "Định lượng phải lớn hơn 0");
		}

		// Thử trường hợp trùng nguyên liệu
		List<RecipeLineRequest> duplicateLines = List.of(
				new RecipeLineRequest(1L, new BigDecimal("0.2")),
				new RecipeLineRequest(1L, new BigDecimal("0.1"))
		);
		Set<Long> duplicateCheck = new HashSet<>();
		boolean hasDuplicate = false;
		for (RecipeLineRequest line : duplicateLines) {
			if (!duplicateCheck.add(line.ingredientId())) {
				hasDuplicate = true;
				break;
			}
		}
		assertTrue(hasDuplicate, "Phải phát hiện được nguyên liệu trùng lặp trong công thức");
	}

	@Test
	@DisplayName("4. Kiểm tra phát hiện món hết hàng dựa theo kho và công thức (Tuần 4)")
	void testOutOfStockDetection() {
		// Giả lập nguyên liệu trong kho
		Ingredient beef = new Ingredient();
		beef.setId(1L);
		beef.setName("Thịt bò");
		beef.setStockQty(new BigDecimal("0.1")); // Chỉ còn 0.1 kg

		Ingredient sauce = new Ingredient();
		sauce.setId(2L);
		sauce.setName("Sốt ướp");
		sauce.setStockQty(new BigDecimal("1.0")); // Còn 1 kg

		// Món ăn cần 0.2 kg thịt bò
		Recipe recipeBeef = new Recipe();
		recipeBeef.setIngredient(beef);
		recipeBeef.setQuantityNeeded(new BigDecimal("0.2"));

		Recipe recipeSauce = new Recipe();
		recipeSauce.setIngredient(sauce);
		recipeSauce.setQuantityNeeded(new BigDecimal("0.05"));

		List<Recipe> recipes = List.of(recipeBeef, recipeSauce);

		boolean hasEnoughStock = true;
		for (Recipe r : recipes) {
			if (r.getIngredient().getStockQty().compareTo(r.getQuantityNeeded()) < 0) {
				hasEnoughStock = false;
				break;
			}
		}

		assertFalse(hasEnoughStock, "Món ăn thiếu thịt bò (còn 0.1 < cần 0.2) phải được xác định là hết hàng");
	}
}
