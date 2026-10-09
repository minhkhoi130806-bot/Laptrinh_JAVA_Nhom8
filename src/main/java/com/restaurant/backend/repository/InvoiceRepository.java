
package com.restaurant.backend.repository;

import com.restaurant.backend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Repository thao tac voi bang invoice
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
}