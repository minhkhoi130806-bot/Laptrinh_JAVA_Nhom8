package com.restaurant.backend.controller;

import com.restaurant.backend.dto.DiningTableResponse;
import com.restaurant.backend.dto.ReservationRequest;
import com.restaurant.backend.dto.ReservationResponse;
import com.restaurant.backend.dto.ReservationStatusRequest;
import com.restaurant.backend.dto.PublicReservationDetailResponse;
import com.restaurant.backend.dto.PublicReservationRequest;
import com.restaurant.backend.entity.Reservation;
import com.restaurant.backend.repository.ReservationRepository;
import java.util.ArrayList;
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
    private final ReservationRepository reservationRepository;

    public ReservationController(ReservationService reservationService,
                                 ReservationMapper reservationMapper,
                                 DiningTableMapper tableMapper,
                                 CustomerRepository customerRepository,
                                 ReservationRepository reservationRepository) {
        this.reservationService = reservationService;
        this.reservationMapper = reservationMapper;
        this.tableMapper = tableMapper;
        this.customerRepository = customerRepository;
        this.reservationRepository = reservationRepository;
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

        // Cập nhật email để nhận thông báo xác nhận đặt bàn qua Gmail nếu khách có nhập email
        if (request.email() != null && !request.email().isBlank()) {
            String newEmail = request.email().trim();
            if (customer.getEmail() == null || customer.getEmail().isBlank() || !customer.getEmail().equalsIgnoreCase(newEmail)) {
                customer.setEmail(newEmail);
                customer = customerRepository.save(customer);
            }
        }
        if (request.fullName() != null && !request.fullName().isBlank()) {
            String newName = request.fullName().trim();
            if (customer.getFullName() == null || customer.getFullName().isBlank() || "Khách vãng lai".equalsIgnoreCase(customer.getFullName())) {
                customer.setFullName(newName);
                customer = customerRepository.save(customer);
            }
        }
        String combinedNote = request.note();
        if (request.preOrderItems() != null && !request.preOrderItems().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Món đặt trước: ");
            for (int i = 0; i < request.preOrderItems().size(); i++) {
                var item = request.preOrderItems().get(i);
                if (i > 0) sb.append(", ");
                sb.append(item.quantity() != null ? item.quantity() : 1)
                  .append("x ")
                  .append(item.itemName() != null ? item.itemName() : "Món");
            }
            if (request.note() != null && !request.note().isBlank()) {
                sb.append(" | Ghi chú: ").append(request.note().trim());
            }
            combinedNote = sb.toString();
            if (combinedNote.length() > 250) {
                combinedNote = combinedNote.substring(0, 247) + "...";
            }
        }

        ReservationRequest reservationRequest = new ReservationRequest(
                customer.getId(), request.tableId(), request.reservedAt(),
                request.guestCount(), combinedNote
        );
        return reservationMapper.toResponse(reservationService.create(reservationRequest));
    }

    /**
     * Tra cứu lịch sử đặt bàn công khai của khách hàng theo số điện thoại hoặc customerId.
     * GET /api/reservations/public/history?phone=0912345678&customerId=1
     */
    @GetMapping("/public/history")
    @Transactional(readOnly = true)
    public List<PublicReservationDetailResponse> getPublicHistory(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Long customerId) {
        if ((phone == null || phone.isBlank()) && customerId == null) {
            return List.of();
        }
        List<Reservation> list = new ArrayList<>();
        if (customerId != null) {
            list.addAll(reservationRepository.findByCustomerIdOrderByReservedAtDesc(customerId));
        }
        if (phone != null && !phone.isBlank()) {
            String clean = phone.trim().replaceAll("[ .-]", "");
            List<Reservation> byPhone = reservationRepository.findByCustomer_PhoneOrderByReservedAtDesc(clean);
            if (byPhone.isEmpty() && !clean.equals(phone.trim())) {
                byPhone = reservationRepository.findByCustomer_PhoneOrderByReservedAtDesc(phone.trim());
            }
            for (Reservation r : byPhone) {
                if (list.stream().noneMatch(existing -> existing.getId().equals(r.getId()))) {
                    list.add(r);
                }
            }
        }
        list.sort((a, b) -> {
            if (a.getReservedAt() == null || b.getReservedAt() == null) return 0;
            return b.getReservedAt().compareTo(a.getReservedAt());
        });

        return list.stream().map(r -> new PublicReservationDetailResponse(
                r.getId(),
                r.getCustomer() != null ? r.getCustomer().getId() : null,
                r.getCustomer() != null ? r.getCustomer().getFullName() : null,
                r.getCustomer() != null ? r.getCustomer().getPhone() : null,
                r.getTable() != null ? r.getTable().getId() : null,
                r.getTable() != null ? r.getTable().getTableCode() : "Bàn đã chọn",
                r.getTable() != null ? r.getTable().getZone() : "Khu vực chung",
                r.getTable() != null ? r.getTable().getCapacity() : (r.getGuestCount() != null ? r.getGuestCount() : 2),
                r.getReservedAt(),
                r.getGuestCount(),
                r.getStatus(),
                r.getNote()
        )).toList();
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
