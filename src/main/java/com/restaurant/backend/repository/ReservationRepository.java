package com.restaurant.backend.repository;

import com.restaurant.backend.entity.Reservation;
import com.restaurant.backend.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByCustomerId(Long customerId);

    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH r.customer LEFT JOIN FETCH r.table WHERE r.customer.id = :customerId ORDER BY r.reservedAt DESC")
    List<Reservation> findByCustomerIdOrderByReservedAtDesc(@Param("customerId") Long customerId);

    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH r.customer LEFT JOIN FETCH r.table WHERE r.customer.phone = :phone ORDER BY r.reservedAt DESC")
    List<Reservation> findByCustomer_PhoneOrderByReservedAtDesc(@Param("phone") String phone);

    List<Reservation> findByTableId(Long tableId);

    // Các lượt có trạng thái nhất định và giờ đặt nằm trong khoảng [from, to]
    List<Reservation> findByStatusAndReservedAtBetween(ReservationStatus status, LocalDateTime from, LocalDateTime to);

    List<Reservation> findByTableIdAndStatusAndReservedAtBetween(
            Long tableId, ReservationStatus status, LocalDateTime from, LocalDateTime to);
}
