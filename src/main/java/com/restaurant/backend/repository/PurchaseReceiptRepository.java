package com.restaurant.backend.repository;

import com.restaurant.backend.entity.PurchaseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceipt, Long> {
    List<PurchaseReceipt> findAllByOrderByReceiptDateDescIdDesc();

    // Thống kê chi phí nhập theo nhà cung cấp: [supplierId, supplierName, count(r), sum(r.totalAmount)]
    @Query("select r.supplier.id, r.supplier.name, count(r), sum(r.totalAmount) "
            + "from PurchaseReceipt r group by r.supplier.id, r.supplier.name order by sum(r.totalAmount) desc")
    List<Object[]> aggregateCostBySupplier();
}
