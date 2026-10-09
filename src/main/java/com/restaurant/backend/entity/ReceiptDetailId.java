package com.restaurant.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Khóa chính kép của bảng RECEIPT_DETAIL: (receipt_id, ingredient_id)
// Nghĩa là trong 1 phiếu, mỗi nguyên liệu chỉ xuất hiện 1 dòng.
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptDetailId implements Serializable {

    @Column(name = "receipt_id")
    private Long receiptId;

    @Column(name = "ingredient_id")
    private Long ingredientId;
}
