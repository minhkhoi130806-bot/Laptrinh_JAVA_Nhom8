package com.restaurant.backend.service;

import com.restaurant.backend.dto.ReservationRequest;
import com.restaurant.backend.entity.Reservation;
import com.restaurant.backend.entity.ReservationStatus;

import java.util.List;

public interface ReservationService {
    Reservation create(ReservationRequest request);

    List<Reservation> getAll(Long customerId, Long tableId);

    Reservation getById(Long id);

    Reservation update(Long id, ReservationRequest request);

    Reservation updateStatus(Long id, ReservationStatus status);

    void delete(Long id);
}
