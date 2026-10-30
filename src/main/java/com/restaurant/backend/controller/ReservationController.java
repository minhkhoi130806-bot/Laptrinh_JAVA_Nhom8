package com.restaurant.backend.controller;

import com.restaurant.backend.dto.DiningTableResponse;
import com.restaurant.backend.dto.ReservationRequest;
import com.restaurant.backend.dto.ReservationResponse;
import com.restaurant.backend.dto.ReservationStatusRequest;
import com.restaurant.backend.dto.PublicReservationRequest;
import com.restaurant.backend.entity.Customer;
import com.restaurant.backend.repository.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import com.restaurant.backend.mapper.DiningTableMapper;
import com.restaurant.backend.mapper.ReservationMapper;
import com.restaurant.backend.service.ReservationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationMapper reservationMapper;
    private final DiningTableMapper tableMapper;
    private final CustomerRepository customerRepository;

    public ReservationController(ReservationService reservationService,
                                 ReservationMapper reservationMapper,
                                 DiningTableMapper tableMapper,
                                 CustomerRepository customerRepository) {
        this.reservationService = reservationService;
        this.reservationMapper = reservationMapper;
        this.tableMapper = tableMapper;
        this.customerRepository = customerRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse addReservation(@RequestBody ReservationRequest request) {
        return reservationMapper.toResponse(reservationService.create(request));
    }

    /**
     * Đặt bàn công khai cho khách từ website.
     * Khách không cần đăng nhập và không cần biết customerId.
     * POST /api/reservations/public
     */
    @PostMapping("/public")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ReservationResponse createPublicReservation(@Valid @RequestBody PublicReservationRequest request) {
        String phone = request.phone().trim();
        Customer customer = customerRepository.findByPhone(phone).orElseGet(() -> {
            Customer created = new Customer();
            created.setFullName(request.fullName().trim());
            created.setPhone(phone);
            created.setEmail(request.email() == null || request.email().isBlank() ? null : request.email().trim());
            created.setLoyaltyPoints(0);
            created.setDeleted(false);
            return customerRepository.save(created);
        });

        // Nếu khách đã tồn tại nhưng bị xóa mềm, đặt bàn mới sẽ kích hoạt lại hồ sơ.
        if (Boolean.TRUE.equals(customer.getDeleted())) {
            customer.setDeleted(false);
            customer.setDeletedAt(null);
            customer = customerRepository.save(customer);
        }
        ReservationRequest reservationRequest = new ReservationRequest(
                customer.getId(), request.tableId(), request.reservedAt(),
                request.guestCount(), request.note()
        );
        return reservationMapper.toResponse(reservationService.create(reservationRequest));
    }

    // GET /api/reservations?customerId=1  hoặc  ?tableId=2
    @GetMapping
    public List<ReservationResponse> getAllReservations(@RequestParam(required = false) Long customerId,
                                                        @RequestParam(required = false) Long tableId) {
        return reservationService.getAll(customerId, tableId).stream().map(reservationMapper::toResponse).toList();
    }

    // GET /api/reservations/available-tables?at=2026-10-20T19:00&guests=4&ignoreId=5
    // -> các bàn trống vào giờ đó và đủ chỗ (dùng cho ô chọn bàn khi đặt)
    @GetMapping("/available-tables")
    public List<DiningTableResponse> availableTables(
            @RequestParam("at") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) Long ignoreId) {
        return reservationService.findAvailableTables(at, guests, ignoreId).stream().map(tableMapper::toResponse).toList();
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

    // POST /api/reservations/{id}/check-in : khách đến -> lượt đặt hoàn tất, bàn chuyển "Đang dùng"
    @PostMapping("/{id}/check-in")
    public ReservationResponse checkIn(@PathVariable Long id) {
        return reservationMapper.toResponse(reservationService.checkIn(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReservation(@PathVariable Long id) {
        reservationService.delete(id);
    }
}
