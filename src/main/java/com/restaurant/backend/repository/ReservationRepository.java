package com.restaurant.backend.repository;

import com.restaurant.backend.entity.Reservation;
import com.restaurant.backend.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByCustomerId(Long customerId);

    List<Reservation> findByTableId(Long tableId);

    // Các lượt có trạng thái nhất định và giờ đặt nằm trong khoảng [from, to]
    List<Reservation> findByStatusAndReservedAtBetween(ReservationStatus status, LocalDateTime from, LocalDateTime to);

    List<Reservation> findByTableIdAndStatusAndReservedAtBetween(
            Long tableId, ReservationStatus status, LocalDateTime from, LocalDateTime to);
}
