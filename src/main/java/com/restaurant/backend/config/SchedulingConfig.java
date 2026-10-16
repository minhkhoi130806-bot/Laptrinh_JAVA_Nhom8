package com.restaurant.backend.config;

import com.restaurant.backend.service.ReservationService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

// Mỗi phút kiểm tra: lượt đặt đã xác nhận sắp đến giờ thì bàn tự chuyển sang "Đã đặt"
@Configuration
@EnableScheduling
public class SchedulingConfig {

    private final ReservationService reservationService;

    public SchedulingConfig(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    public void syncReservedTables() {
        reservationService.syncTableStatuses();
    }
}
