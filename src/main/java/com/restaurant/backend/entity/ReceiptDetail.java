package com.restaurant.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

// Chi tiết phiếu nhập: nhập nguyên liệu nào, số lượng bao nhiêu, giá nhập bao nhiêu
@Entity
@Table(name = "receipt_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptDetail {

    // Khóa chính kép (receipt_id + ingredient_id)
    @EmbeddedId
    private ReceiptDetailId id = new ReceiptDetailId();

    // N dòng chi tiết - 1 phiếu nhập
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("receiptId")
    @JoinColumn(name = "receipt_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PurchaseReceipt receipt;

    // N dòng chi tiết - 1 nguyên liệu
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("ingredientId")
    @JoinColumn(name = "ingredient_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Ingredient ingredient;

    // Số lượng nhập (sẽ cộng vào Ingredient.stockQty ở tuần 3)
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    // Đơn giá nhập tại thời điểm nhập
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
}
