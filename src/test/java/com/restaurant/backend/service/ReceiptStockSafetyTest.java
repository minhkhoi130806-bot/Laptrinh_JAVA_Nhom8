package com.restaurant.backend.service;

import com.restaurant.backend.dto.ReceiptDetailRequest;
import com.restaurant.backend.dto.ReceiptRequest;
import com.restaurant.backend.entity.Employee;
import com.restaurant.backend.entity.Ingredient;
import com.restaurant.backend.entity.PurchaseReceipt;
import com.restaurant.backend.entity.Supplier;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.mapper.ReceiptMapper;
import com.restaurant.backend.repository.EmployeeRepository;
import com.restaurant.backend.repository.IngredientRepository;
import com.restaurant.backend.repository.PurchaseReceiptRepository;
import com.restaurant.backend.repository.SupplierRepository;
import com.restaurant.backend.service.Impl.ReceiptServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ReceiptStockSafetyTest {
    @Mock PurchaseReceiptRepository receipts;
    @Mock SupplierRepository suppliers;
    @Mock EmployeeRepository employees;
    @Mock IngredientRepository ingredients;
    @Mock ReceiptMapper mapper;
    @Mock EntityManager entityManager;
    @InjectMocks ReceiptServiceImpl service;

    @Test
    void receiptLocksIngredientsInIdOrderAndAddsToRefreshedStock() {
        when(suppliers.findById(1L)).thenReturn(Optional.of(new Supplier()));
        Ingredient first = ingredient(3, "5");
        Ingredient second = ingredient(7, "4");
        when(ingredients.findByIdForUpdate(3L)).thenReturn(Optional.of(first));
        when(ingredients.findByIdForUpdate(7L)).thenReturn(Optional.of(second));
        // Simulate another completed transaction having reduced the cached stock from 5 to 3.
        doAnswer(invocation -> { first.setStockQty(new BigDecimal("3")); return null; })
                .when(entityManager).refresh(first, LockModeType.PESSIMISTIC_WRITE);
        when(receipts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReceiptRequest request = new ReceiptRequest(1L, null, null, null,
                List.of(detail(7, "1", "200"), detail(3, "2", "100")));

        service.create(request);

        assertEquals(new BigDecimal("5"), first.getStockQty());
        assertEquals(new BigDecimal("5"), second.getStockQty());
        var order = inOrder(ingredients, entityManager);
        order.verify(ingredients).findByIdForUpdate(3L);
        order.verify(entityManager).refresh(first, LockModeType.PESSIMISTIC_WRITE);
        order.verify(ingredients).findByIdForUpdate(7L);
        order.verify(entityManager).refresh(second, LockModeType.PESSIMISTIC_WRITE);
        ArgumentCaptor<PurchaseReceipt> saved = ArgumentCaptor.forClass(PurchaseReceipt.class);
        verify(receipts).save(saved.capture());
        assertEquals(new BigDecimal("400"), saved.getValue().getTotalAmount());
        assertEquals(2, saved.getValue().getDetails().size());
    }

    @Test
    void invalidLaterLineDoesNotChangeAnyIngredient() {
        when(suppliers.findById(1L)).thenReturn(Optional.of(new Supplier()));
        ReceiptRequest request = new ReceiptRequest(1L, null, null, null,
                List.of(detail(3, "2", "100"), detail(7, "-1", "200")));

        assertThrows(BadRequestException.class, () -> service.create(request));

        verifyNoInteractions(ingredients, entityManager, receipts);
    }

    @Test
    void receiptRejectsInactiveEmployee() {
        when(suppliers.findById(1L)).thenReturn(Optional.of(new Supplier()));
        Employee employee = new Employee();
        employee.setDeleted(true);
        when(employees.findById(2L)).thenReturn(Optional.of(employee));
        ReceiptRequest request = new ReceiptRequest(1L, 2L, null, null, List.of(detail(3, "2", "100")));

        assertThrows(BadRequestException.class, () -> service.create(request));

        verifyNoInteractions(ingredients, receipts);
    }

    private static ReceiptDetailRequest detail(long id, String quantity, String price) {
        return new ReceiptDetailRequest(id, new BigDecimal(quantity), new BigDecimal(price));
    }

    private static Ingredient ingredient(long id, String stock) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(id);
        ingredient.setStockQty(new BigDecimal(stock));
        return ingredient;
    }
}
