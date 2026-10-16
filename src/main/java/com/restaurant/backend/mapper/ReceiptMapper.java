package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.ReceiptDetailResponse;
import com.restaurant.backend.dto.ReceiptResponse;
import com.restaurant.backend.entity.PurchaseReceipt;
import com.restaurant.backend.entity.ReceiptDetail;
import org.springframework.stereotype.Component;

import java.util.List;

// Gọi trong service (có transaction) vì supplier / employee / details là LAZY
@Component
public class ReceiptMapper {

    public ReceiptResponse toResponse(PurchaseReceipt r) {
        List<ReceiptDetailResponse> details = r.getDetails().stream().map(this::toResponse).toList();
        return new ReceiptResponse(
                r.getId(),
                r.getSupplier().getId(),
                r.getSupplier().getName(),
                r.getEmployee() != null ? r.getEmployee().getId() : null,
                r.getEmployee() != null ? r.getEmployee().getFullName() : null,
                r.getReceiptDate(),
                r.getTotalAmount(),
                r.getNote(),
                details
        );
    }

    private ReceiptDetailResponse toResponse(ReceiptDetail d) {
        return new ReceiptDetailResponse(
                d.getIngredient().getId(),
                d.getIngredient().getName(),
                d.getIngredient().getUnit(),
                d.getQuantity(),
                d.getUnitPrice(),
                d.getQuantity().multiply(d.getUnitPrice())
        );
    }
}
