package com.restaurant.backend.service;

import com.restaurant.backend.entity.*;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_integrity;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "app.security.jwt-secret=payment-test-secret-at-least-32-bytes-long",
        "app.mail.enabled=false",
        "app.scheduling.enabled=false"
})
class CheckoutConcurrencyIntegrationTest {
    @Autowired private InvoiceService checkout;
    @Autowired private InvoiceRepository invoices;
    @Autowired private OrderRepository orders;
    @Autowired private OrderItemRepository orderItems;
    @Autowired private IngredientRepository ingredients;
    @Autowired private RecipeRepository recipes;
    @Autowired private MenuItemRepository menu;
    @Autowired private CategoryRepository categories;
    @Autowired private CustomerRepository customers;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private EmailService email;

    @Test
    void concurrentCheckoutOfTheSameOrderCreatesOneInvoiceAndConsumesStockOnce() throws Exception {
        Fixture fixture = fixture("2.0", "0.4", 1, true);
        int orderId = fixture.orderIds().get(0);

        List<Boolean> results = runTogether(List.of(orderId, orderId));

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(1, invoices.findByOrderId(orderId).size());
        assertEquals("PAID", orders.findById(orderId).orElseThrow().getStatus());
        assertEquals(0, new BigDecimal("1.6").compareTo(ingredients.findById(fixture.ingredientId()).orElseThrow().getStockQty()));
        assertEquals(60, customers.findById(fixture.customerId()).orElseThrow().getLoyaltyPoints());
    }

    @Test
    void concurrentOrdersCompetingForOneIngredientCannotOverspendTheStock() throws Exception {
        Fixture fixture = fixture("1.0", "0.7", 2, false);

        List<Boolean> results = runTogether(fixture.orderIds());

        assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(0, new BigDecimal("0.3").compareTo(ingredients.findById(fixture.ingredientId()).orElseThrow().getStockQty()));
        assertEquals(1, fixture.orderIds().stream().filter(id -> "PAID".equals(orders.findById(id).orElseThrow().getStatus())).count());
        assertEquals(1, fixture.orderIds().stream().filter(id -> "OPEN".equals(orders.findById(id).orElseThrow().getStatus())).count());
        assertEquals(1, fixture.orderIds().stream().mapToInt(id -> invoices.findByOrderId(id).size()).sum());
    }

    @Test
    void concurrentPaymentsForTheSameCustomerDoNotLoseLoyaltyPoints() throws Exception {
        // No recipe: these orders only contend on the customer, not on an ingredient lock.
        Fixture fixture = fixture("0", null, 2, true);

        List<Boolean> results = runTogether(fixture.orderIds());

        assertTrue(results.stream().allMatch(Boolean::booleanValue));
        assertEquals(70, customers.findById(fixture.customerId()).orElseThrow().getLoyaltyPoints());
        assertEquals(2, fixture.orderIds().stream().mapToInt(id -> invoices.findByOrderId(id).size()).sum());
    }

    private List<Boolean> runTogether(List<Integer> orderIds) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(orderIds.size());
        CountDownLatch ready = new CountDownLatch(orderIds.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Integer id : orderIds) {
                futures.add(workers.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Checkout start timed out");
                    try {
                        checkout.checkout(new com.restaurant.backend.dto.CheckoutRequest(id, null, "CASH"));
                        return true;
                    } catch (BadRequestException expectedBusinessRejection) {
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Both checkout requests must be ready");
            start.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) results.add(future.get(30, TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private Fixture fixture(String stock, String quantityNeeded, int orderCount, boolean withCustomer) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = UUID.randomUUID().toString();
            Category category = new Category();
            category.setName("Checkout category " + suffix);
            category = categories.save(category);
            Ingredient ingredient = new Ingredient();
            ingredient.setName("Checkout ingredient " + suffix);
            ingredient.setUnit("kg");
            ingredient.setStockQty(new BigDecimal(stock));
            ingredient = ingredients.save(ingredient);
            Menuitem dish = new Menuitem();
            dish.setCategory(category);
            dish.setNameVi("Checkout dish " + suffix);
            dish.setPrice(new BigDecimal("100000"));
            dish = menu.save(dish);
            if (quantityNeeded != null) {
                Recipe recipe = new Recipe();
                recipe.setId(new RecipeId(dish.getId(), ingredient.getId()));
                recipe.setMenuItem(dish);
                recipe.setIngredient(ingredient);
                recipe.setQuantityNeeded(new BigDecimal(quantityNeeded));
                recipes.save(recipe);
            }
            Customer customer = null;
            if (withCustomer) {
                customer = new Customer();
                customer.setFullName("Checkout customer");
                customer.setPhone("09" + String.format("%09d", Math.floorMod(suffix.hashCode(), 1_000_000_000)));
                customer.setLoyaltyPoints(50);
                customer = customers.save(customer);
            }
            List<Integer> orderIds = new ArrayList<>();
            for (int index = 0; index < orderCount; index++) {
                Order order = new Order();
                order.setCreatedAt(LocalDateTime.now());
                order.setStatus("OPEN");
                if (customer != null) order.setCustomerId(Math.toIntExact(customer.getId()));
                order = orders.save(order);
                OrderItem item = new OrderItem();
                item.setId(new OrderItemId(order.getId(), Math.toIntExact(dish.getId())));
                item.setQuantity(1);
                item.setUnitPrice(dish.getPrice());
                orderItems.save(item);
                orderIds.add(order.getId());
            }
            return new Fixture(ingredient.getId(), customer == null ? null : customer.getId(), orderIds);
        });
    }

    private record Fixture(Long ingredientId, Long customerId, List<Integer> orderIds) {}
}
