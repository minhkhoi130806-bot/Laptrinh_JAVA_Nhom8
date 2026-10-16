package com.restaurant.backend.service;

import com.restaurant.backend.dto.ReservationRequest;
import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.Reservation;
import com.restaurant.backend.entity.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationService {
    Reservation create(ReservationRequest request);
    List<Reservation> getAll(Long customerId, Long tableId);
    Reservation getById(Long id);
    Reservation update(Long id, ReservationRequest request);
    Reservation updateStatus(Long id, ReservationStatus status);
    void delete(Long id);

    // Khách đến: lượt đặt -> HOÀN TẤT, bàn -> ĐANG DÙNG
    Reservation checkIn(Long id);

    // Các bàn còn trống vào giờ `at` và đủ chỗ cho `guests` người
    // (ignoreReservationId: bỏ qua lượt đang sửa để không tự va chạm với chính nó)
    List<DiningTable> findAvailableTables(LocalDateTime at, Integer guests, Long ignoreReservationId);

    // Lượt đã xác nhận sắp đến giờ -> chuyển bàn sang ĐÃ ĐẶT (chạy định kỳ mỗi phút)
    void syncTableStatuses();
}
