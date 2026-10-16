package com.restaurant.backend.repository;

import com.restaurant.backend.entity.PurchaseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceipt, Long> {
    List<PurchaseReceipt> findAllByOrderByReceiptDateDescIdDesc();
}
