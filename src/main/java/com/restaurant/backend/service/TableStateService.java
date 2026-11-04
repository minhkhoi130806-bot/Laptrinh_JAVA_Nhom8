package com.restaurant.backend.service;

import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.ReservationStatus;
import com.restaurant.backend.entity.TableStatus;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.DiningTableRepository;
import com.restaurant.backend.repository.OrderRepository;
import com.restaurant.backend.repository.ReservationRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TableStateService {
    private final DiningTableRepository tables;
    private final OrderRepository orders;
    private final ReservationRepository reservations;
    private final EntityManager entityManager;

    public TableStateService(DiningTableRepository tables, OrderRepository orders,
                             ReservationRepository reservations, EntityManager entityManager) {
        this.tables = tables;
        this.orders = orders;
        this.reservations = reservations;
        this.entityManager = entityManager;
    }

    public boolean hasActiveOrders(Integer tableId) {
        return tableId != null && orders.existsByTableIdAndStatusIn(tableId, List.of("OPEN", "SERVING"));
    }

    @Transactional
    public void refresh(Integer tableId) {
        if (tableId == null) return;
        entityManager.flush();
        DiningTable table = tables.findByIdForUpdate(tableId.longValue())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bàn " + tableId));
        entityManager.refresh(table);
        LocalDateTime now = LocalDateTime.now();
        boolean held = !reservations.findByTableIdAndStatusAndReservedAtBetween(
                table.getId(), ReservationStatus.CONFIRMED, now.minusMinutes(30), now.plusMinutes(30)).isEmpty();
        table.setStatus(hasActiveOrders(tableId) ? TableStatus.OCCUPIED
                : held ? TableStatus.RESERVED : TableStatus.AVAILABLE);
        tables.save(table);
    }
}
