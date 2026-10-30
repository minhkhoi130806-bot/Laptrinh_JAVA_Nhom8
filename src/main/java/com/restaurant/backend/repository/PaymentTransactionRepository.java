package com.restaurant.backend.repository;

import com.restaurant.backend.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    /** Khóa dòng (SELECT ... FOR UPDATE) để IPN trùng/đồng thời chỉ xử lý được một lần. */
    // Native SQL avoids Hibernate emitting dialect-specific "FOR UPDATE OF alias"
    // syntax that MariaDB does not accept. Call only inside an active transaction.
    @Query(value = "SELECT * FROM payment_transaction WHERE idempotency_key = :key FOR UPDATE", nativeQuery = true)
    Optional<PaymentTransaction> findWithLockByIdempotencyKey(@Param("key") String idempotencyKey);

    List<PaymentTransaction> findByOrderIdAndProviderAndStatus(Long orderId, String provider, String status);

    List<PaymentTransaction> findByProviderAndStatusAndCreatedAtBetween(String provider, String status, LocalDateTime from, LocalDateTime to);

    List<PaymentTransaction> findByProviderAndStatusAndInvoiceIdIsNullAndCreatedAtAfter(String provider, String status, LocalDateTime after);
}
