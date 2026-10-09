package com.restaurant.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Phiếu nhập hàng từ nhà cung cấp (1 phiếu - nhiều dòng chi tiết)
@Entity
@Table(name = "purchase_receipt")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // N phiếu nhập - 1 nhà cung cấp (Supplier 1-N PurchaseReceipt)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Supplier supplier;

    // Nhân viên lập phiếu (có thể để trống nếu ERD của nhóm không có cột này)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Employee employee;

    @Column(name = "receipt_date", nullable = false)
    private LocalDateTime receiptDate;

    // Tổng tiền phiếu = tổng (quantity * unitPrice) các dòng chi tiết
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 255)
    private String note;

    // 1 phiếu - N dòng chi tiết. Lưu/xóa phiếu thì lưu/xóa luôn chi tiết
    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ReceiptDetail> details = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (receiptDate == null) {
            receiptDate = LocalDateTime.now();
        }
    }

    // Hàm tiện ích: thêm 1 dòng chi tiết và gắn 2 chiều (tránh quên set receipt)
    public void addDetail(ReceiptDetail detail) {
        detail.setReceipt(this);
        this.details.add(detail);
    }
}
