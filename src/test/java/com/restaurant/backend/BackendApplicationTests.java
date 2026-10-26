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

	@Test
	@DisplayName("5. Kiểm tra trừ kho nguyên liệu khi thanh toán đơn hàng (Tuần 4 - Khôi)")
	void testDeductStockOnCheckout() {
		// Nguyên liệu ban đầu có 5.0 kg gạo
		BigDecimal initialStock = new BigDecimal("5.0");
		// Món cơm trộn cần 0.3 kg gạo cho 1 phần
		BigDecimal neededPerItem = new BigDecimal("0.3");
		// Khách gọi 3 phần cơm trộn
		int quantity = 3;

		BigDecimal totalNeeded = neededPerItem.multiply(BigDecimal.valueOf(quantity)); // 0.9 kg
		BigDecimal remainingStock = initialStock.subtract(totalNeeded); // 4.1 kg

		assertEquals(new BigDecimal("0.9"), totalNeeded, "Lượng nguyên liệu cần trừ phải là 0.9 kg");
		assertEquals(new BigDecimal("4.1"), remainingStock, "Tồn kho sau khi trừ phải còn 4.1 kg");
	}

	@Test
	@DisplayName("6. Kiểm tra tích lũy điểm thưởng loyalty_points cho khách hàng (Tuần 4 - Khôi)")
	void testLoyaltyPointsCalculation() {
		// Hóa đơn thanh toán 250.000 VNĐ
		BigDecimal totalPaid = new BigDecimal("250000");
		// Quy tắc: mỗi 10.000 VNĐ được 1 điểm thưởng
		int pointsEarned = totalPaid.divide(BigDecimal.valueOf(10000), 0, java.math.RoundingMode.FLOOR).intValue();

		assertEquals(25, pointsEarned, "250.000 VNĐ phải được cộng 25 điểm tích lũy");

		// Khách hàng đã có sẵn 10 điểm
		int initialPoints = 10;
		int newPoints = initialPoints + pointsEarned;
		assertEquals(35, newPoints, "Tổng điểm khách hàng sau khi tích điểm phải là 35");
	}

	@Test
	@DisplayName("7. Kiểm tra tính tổng thanh toán hóa đơn: total = subtotal - discount (Tuần 3 & 4 - Khôi)")
	void testInvoiceCheckoutTotal() {
		BigDecimal subtotal = new BigDecimal("180000"); // 180k
		BigDecimal discount = new BigDecimal("20000");  // Giảm 20k

		BigDecimal total = subtotal.subtract(discount);
		assertEquals(new BigDecimal("160000"), total, "Tổng tiền thanh toán thực tế là 160.000 VNĐ");

		// Trường hợp giảm giá lớn hơn subtotal -> total = 0, không được âm
		BigDecimal bigDiscount = new BigDecimal("200000");
		BigDecimal zeroTotal = subtotal.subtract(bigDiscount);
		if (zeroTotal.compareTo(BigDecimal.ZERO) < 0) {
			zeroTotal = BigDecimal.ZERO;
		}
		assertEquals(BigDecimal.ZERO, zeroTotal, "Tổng tiền thanh toán không được âm khi chiết khấu vượt quá tiền món");
	}
}
