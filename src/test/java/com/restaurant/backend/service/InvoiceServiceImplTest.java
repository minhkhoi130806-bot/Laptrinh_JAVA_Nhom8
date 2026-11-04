package com.restaurant.backend.service;

import com.restaurant.backend.dto.CheckoutRequest;
import com.restaurant.backend.dto.RevenueReportResponse;
import com.restaurant.backend.entity.*;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.repository.*;
import com.restaurant.backend.service.Impl.InvoiceServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InvoiceServiceImplTest {
    private InvoiceRepository invoices;
    private OrderRepository orders;
    private OrderItemRepository items;
    private TableStateService tables;
    private RecipeRepository recipes;
    private IngredientRepository ingredients;
    private CustomerRepository customers;
    private EmailService email;
    private EntityManager entityManager;
    private InvoiceServiceImpl service;
    private Order order;

    @BeforeEach
    void setUp() {
        invoices = mock(InvoiceRepository.class);
        orders = mock(OrderRepository.class);
        items = mock(OrderItemRepository.class);
        tables = mock(TableStateService.class);
        recipes = mock(RecipeRepository.class);
        ingredients = mock(IngredientRepository.class);
        customers = mock(CustomerRepository.class);
        email = mock(EmailService.class);
        entityManager = mock(EntityManager.class);
        service = new InvoiceServiceImpl(invoices, orders, items, tables, recipes, ingredients, customers, email, entityManager);
        order = new Order();
        order.setId(1);
        order.setStatus("OPEN");
        order.setTableId(3);
        when(orders.findByIdForUpdate(1)).thenReturn(Optional.of(order));
        when(invoices.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rejectsAggregateIngredientShortageWithoutChangingAnything() {
        Ingredient beef = ingredient(1L, "Thịt bò", "1.0");
        when(items.findByOrderIdForUpdate(1)).thenReturn(List.of(item(10, 1, "100000"), item(11, 1, "150000")));
        when(recipes.findByMenuItem(10L)).thenReturn(List.of(recipe(beef, "0.7")));
        when(recipes.findByMenuItem(11L)).thenReturn(List.of(recipe(beef, "0.7")));
        when(ingredients.findByIdForUpdate(1L)).thenReturn(Optional.of(beef));

        BadRequestException failure = assertThrows(BadRequestException.class,
                () -> service.checkout(new CheckoutRequest(1, null, "CASH")));

        assertTrue(failure.getMessage().contains("cần 1.4"));
        assertEquals(new BigDecimal("1.0"), beef.getStockQty());
        assertEquals("OPEN", order.getStatus());
        verify(invoices, never()).save(any());
        verify(ingredients, never()).save(any());
        verify(orders, never()).save(any());
        verifyNoInteractions(customers, tables, email);
    }

    @Test
    void checksEveryIngredientBeforeConsumingAnyStockAndLocksInIdOrder() {
        Ingredient first = ingredient(1L, "Bò", "10");
        Ingredient second = ingredient(2L, "Sốt", "0");
        when(items.findByOrderIdForUpdate(1)).thenReturn(List.of(item(10, 1, "100000")));
        when(recipes.findByMenuItem(10L)).thenReturn(List.of(recipe(second, "0.5"), recipe(first, "1")));
        when(ingredients.findByIdForUpdate(1L)).thenReturn(Optional.of(first));
        when(ingredients.findByIdForUpdate(2L)).thenReturn(Optional.of(second));

        assertThrows(BadRequestException.class, () -> service.checkout(new CheckoutRequest(1, null, "CASH")));

        var locks = inOrder(ingredients);
        locks.verify(ingredients).findByIdForUpdate(1L);
        locks.verify(ingredients).findByIdForUpdate(2L);
        verify(ingredients, never()).save(any());
        verify(invoices, never()).save(any());
        assertEquals(new BigDecimal("10"), first.getStockQty());
    }

    @Test
    void successfulCheckoutUsesSharedRecipeTotalsActualPaymentAndCustomerPoints() {
        Ingredient beef = ingredient(1L, "Thịt bò", "2.0");
        Customer customer = new Customer();
        customer.setId(7L);
        customer.setLoyaltyPoints(50);
        order.setCustomerId(7);
        when(items.findByOrderIdForUpdate(1)).thenReturn(List.of(item(10, 2, "100000"), item(11, 1, "50000")));
        when(recipes.findByMenuItem(10L)).thenReturn(List.of(recipe(beef, "0.2")));
        when(recipes.findByMenuItem(11L)).thenReturn(List.of(recipe(beef, "0.1")));
        when(ingredients.findByIdForUpdate(1L)).thenReturn(Optional.of(beef));
        when(customers.findByIdForUpdate(7L)).thenReturn(Optional.of(customer));

        Invoice invoice = service.checkout(new CheckoutRequest(1, new BigDecimal("25000"), "cash"));

        assertEquals(new BigDecimal("250000"), invoice.getSubtotal());
        assertEquals(new BigDecimal("225000"), invoice.getTotal());
        assertEquals("CASH", invoice.getPaymentMethod());
        assertEquals(new BigDecimal("1.5"), beef.getStockQty());
        assertEquals(72, customer.getLoyaltyPoints());
        assertEquals("PAID", order.getStatus());
        verify(ingredients, times(1)).save(beef);
        verify(customers).save(customer);
        verify(entityManager).refresh(beef, LockModeType.PESSIMISTIC_WRITE);
        verify(entityManager).refresh(customer, LockModeType.PESSIMISTIC_WRITE);
        verify(tables).refresh(3);
        verify(email).sendPaymentSuccessEmail(eq(order), eq(invoice), anyList());
    }

    @Test
    void alreadyPaidOrderCannotBeChargedAgain() {
        order.setStatus("PAID");
        assertThrows(BadRequestException.class, () -> service.checkout(new CheckoutRequest(1, null, "MOMO")));
        verify(invoices, never()).save(any());
        verifyNoInteractions(items, ingredients, customers, tables, email);
    }

    @Test
    void existingInvoicePreventsDuplicateEvenWhenLegacyOrderStatusIsOpen() {
        when(invoices.existsByOrderId(1)).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.checkout(new CheckoutRequest(1, null, "VNPAY")));
        verify(invoices, never()).save(any());
        verifyNoInteractions(items, ingredients, customers, tables, email);
    }

    @Test
    void cancelledOrderCannotBePaid() {
        order.setStatus("CANCELLED");
        assertThrows(BadRequestException.class, () -> service.checkout(new CheckoutRequest(1, null, "CASH")));
        verify(invoices, never()).save(any());
        verifyNoInteractions(items, ingredients, customers, tables, email);
    }

    @Test
    void rejectsInvalidDiscountOrUnknownPaymentMethodBeforeWriting() {
        when(items.findByOrderIdForUpdate(1)).thenReturn(List.of(item(10, 1, "100000")));
        when(recipes.findByMenuItem(10L)).thenReturn(List.of());
        assertThrows(BadRequestException.class,
                () -> service.checkout(new CheckoutRequest(1, new BigDecimal("-1"), "CASH")));
        assertThrows(BadRequestException.class,
                () -> service.checkout(new CheckoutRequest(1, new BigDecimal("100001"), "CASH")));
        assertThrows(BadRequestException.class,
                () -> service.checkout(new CheckoutRequest(1, BigDecimal.ZERO, "UNKNOWN")));
        verify(invoices, never()).save(any());
        verifyNoInteractions(ingredients, customers, tables, email);
    }

    @Test
    void manualInvoiceCreationCannotBypassCheckoutCalculations() {
        when(items.findByOrderIdForUpdate(1)).thenReturn(List.of(item(10, 2, "100000")));
        when(recipes.findByMenuItem(10L)).thenReturn(List.of());
        Invoice supplied = new Invoice();
        supplied.setOrderId(1);
        supplied.setTotal(BigDecimal.ONE);
        supplied.setSubtotal(BigDecimal.ONE);

        Invoice saved = service.create(supplied);

        assertEquals(new BigDecimal("200000"), saved.getTotal());
        assertEquals("PAID", order.getStatus());
        verify(tables).refresh(3);
    }

    @Test
    void issuedInvoicesCannotBeChangedOrDeletedWithoutReversingTheirBusinessEffects() {
        Invoice issued = new Invoice();
        issued.setId(20);
        when(invoices.findById(20)).thenReturn(Optional.of(issued));
        assertThrows(BadRequestException.class, () -> service.update(20, new Invoice()));
        assertThrows(BadRequestException.class, () -> service.delete(20));
        verify(invoices, never()).save(any());
        verify(invoices, never()).delete(any());
    }

    @Test
    void paymentEmailWaitsUntilCommitAndIsNotSentOnRollback() {
        when(items.findByOrderIdForUpdate(1)).thenReturn(List.of(item(10, 1, "100000")));
        when(recipes.findByMenuItem(10L)).thenReturn(List.of());
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.checkout(new CheckoutRequest(1, null, "CASH"));
            verifyNoInteractions(email);
            List<TransactionSynchronization> callbacks = TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, callbacks.size());
            callbacks.forEach(callback -> callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verifyNoInteractions(email);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void allRevenueMetricsUseTheSameInclusiveStartExclusiveEndPeriod() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(6).atStartOfDay();
        LocalDateTime until = today.plusDays(1).atStartOfDay();
        when(invoices.findByPaidAtGreaterThanEqualAndPaidAtLessThan(from, until)).thenReturn(List.of(
                invoice("100000", "CASH", from),
                invoice("300000", "MOMO", today.atTime(12, 0)),
                invoice("9000000", "CASH", from.minusNanos(1)),
                invoice("8000000", "CARD", until),
                invoice("7000000", "CARD", null)
        ));

        RevenueReportResponse report = service.getRevenueReport(7);

        assertEquals(new BigDecimal("400000"), report.totalRevenue());
        assertEquals(2L, report.totalInvoices());
        assertEquals(new BigDecimal("200000"), report.averageInvoiceValue());
        assertEquals(new BigDecimal("300000"), report.todayRevenue());
        assertEquals(7, report.dailyRevenues().size());
        assertEquals(new BigDecimal("400000"), report.dailyRevenues().stream()
                .map(RevenueReportResponse.DailyRevenueItem::revenue).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(new BigDecimal("400000"), report.paymentMethodRevenues().stream()
                .map(RevenueReportResponse.PaymentMethodRevenueItem::revenue).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(2, report.paymentMethodRevenues().size());
        verify(invoices).findByPaidAtGreaterThanEqualAndPaidAtLessThan(from, until);
        verify(invoices, never()).findAll();
    }

    @Test
    void invalidReportWindowsAreRejectedBeforeQueryingTheDatabase() {
        assertThrows(BadRequestException.class, () -> service.getRevenueReport(0));
        assertThrows(BadRequestException.class, () -> service.getRevenueReport(367));
        verifyNoInteractions(invoices);
    }

    private OrderItem item(int menuId, int quantity, String price) {
        OrderItem item = new OrderItem();
        item.setId(new OrderItemId(1, menuId));
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(price));
        return item;
    }

    private Ingredient ingredient(long id, String name, String stock) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(id);
        ingredient.setName(name);
        ingredient.setUnit("kg");
        ingredient.setStockQty(new BigDecimal(stock));
        return ingredient;
    }

    private Recipe recipe(Ingredient ingredient, String quantity) {
        Recipe recipe = new Recipe();
        recipe.setIngredient(ingredient);
        recipe.setQuantityNeeded(new BigDecimal(quantity));
        return recipe;
    }

    private Invoice invoice(String total, String method, LocalDateTime paidAt) {
        Invoice invoice = new Invoice();
        invoice.setTotal(new BigDecimal(total));
        invoice.setPaymentMethod(method);
        invoice.setPaidAt(paidAt);
        return invoice;
    }
}
