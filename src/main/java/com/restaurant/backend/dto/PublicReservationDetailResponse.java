package com.restaurant.backend.dto;

import com.restaurant.backend.entity.ReservationStatus;
import java.time.LocalDateTime;

/**
 * DTO trả về chi tiết lịch sử đặt bàn cho khách hàng (kèm thông tin bàn, khu vực và trạng thái).
 */
public record PublicReservationDetailResponse(
        Long id,
        Long customerId,
        String customerName,
        String customerPhone,
        Long tableId,
        String tableCode,
        String zone,
        Integer capacity,
        LocalDateTime reservedAt,
        Integer guestCount,
        ReservationStatus status,
        String note
) {
}
