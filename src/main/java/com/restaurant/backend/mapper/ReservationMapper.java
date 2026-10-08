package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.ReservationRequest;
import com.restaurant.backend.dto.ReservationResponse;
import com.restaurant.backend.entity.Customer;
import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.Reservation;
import com.restaurant.backend.entity.ReservationStatus;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    // customer và table do service tìm ra từ customerId / tableId trong request
    public Reservation toEntity(ReservationRequest request, Customer customer, DiningTable table) {
        if (request == null) {
            return null;
        }
        Reservation reservation = new Reservation();
        reservation.setCustomer(customer);
        reservation.setTable(table);
        reservation.setReservedAt(request.reservedAt());
        reservation.setGuestCount(request.guestCount());
        reservation.setStatus(ReservationStatus.PENDING);
        return reservation;
    }

    // getId() trên proxy LAZY không kích hoạt query nên an toàn ngoài transaction
    public ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getCustomer().getId(),
                reservation.getTable().getId(),
                reservation.getReservedAt(),
                reservation.getGuestCount(),
                reservation.getStatus()
        );
    }
}
