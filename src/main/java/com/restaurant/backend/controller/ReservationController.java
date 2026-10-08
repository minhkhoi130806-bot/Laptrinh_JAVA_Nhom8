package com.restaurant.backend.controller;

import com.restaurant.backend.dto.ReservationRequest;
import com.restaurant.backend.dto.ReservationResponse;
import com.restaurant.backend.dto.ReservationStatusRequest;
import com.restaurant.backend.mapper.ReservationMapper;
import com.restaurant.backend.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationMapper reservationMapper;

    public ReservationController(ReservationService reservationService, ReservationMapper reservationMapper) {
        this.reservationService = reservationService;
        this.reservationMapper = reservationMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse addReservation(@RequestBody ReservationRequest request) {
        return reservationMapper.toResponse(reservationService.create(request));
    }

    // GET /api/reservations?customerId=1  hoặc  ?tableId=2
    @GetMapping
    public List<ReservationResponse> getAllReservations(@RequestParam(required = false) Long customerId,
                                                        @RequestParam(required = false) Long tableId) {
        return reservationService.getAll(customerId, tableId).stream().map(reservationMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ReservationResponse getReservation(@PathVariable Long id) {
        return reservationMapper.toResponse(reservationService.getById(id));
    }

    @PutMapping("/{id}")
    public ReservationResponse updateReservation(@PathVariable Long id, @RequestBody ReservationRequest request) {
        return reservationMapper.toResponse(reservationService.update(id, request));
    }

    // PATCH /api/reservations/{id}/status  body: {"status":"CONFIRMED"}
    @PatchMapping("/{id}/status")
    public ReservationResponse updateStatus(@PathVariable Long id, @RequestBody ReservationStatusRequest request) {
        return reservationMapper.toResponse(reservationService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReservation(@PathVariable Long id) {
        reservationService.delete(id);
    }
}
