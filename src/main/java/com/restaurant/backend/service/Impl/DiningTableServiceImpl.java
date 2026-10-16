package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.ListScope;
import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.ReservationStatus;
import com.restaurant.backend.entity.TableStatus;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.DiningTableRepository;
import com.restaurant.backend.repository.ReservationRepository;
import com.restaurant.backend.service.DiningTableService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiningTableServiceImpl implements DiningTableService {

    private final DiningTableRepository tableRepository;
    private final ReservationRepository reservationRepository;

    public DiningTableServiceImpl(DiningTableRepository tableRepository, ReservationRepository reservationRepository) {
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
    }

    @Override
    @Transactional
    public DiningTable create(DiningTable table) {
        validate(table);
        if (tableRepository.existsByTableCode(table.getTableCode())) {
            throw new BadRequestException("Mã bàn đã tồn tại");
        }
        table.setId(null);
        return tableRepository.save(table);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiningTable> getAll(TableStatus status, ListScope scope) {
        List<DiningTable> tables = scope == ListScope.ALL
                ? tableRepository.findAll()
                : tableRepository.findByDeleted(scope == ListScope.TRASH);
        if (status == null) {
            return tables;
        }
        return tables.stream().filter(t -> t.getStatus() == status).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DiningTable getById(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bàn id = " + id));
    }

    @Override
    @Transactional
    public DiningTable update(Long id, DiningTable data) {
        validate(data);
        DiningTable existing = getById(id);
        if (tableRepository.existsByTableCodeAndIdNot(data.getTableCode(), id)) {
            throw new BadRequestException("Mã bàn đã tồn tại");
        }
        existing.setTableCode(data.getTableCode());
        existing.setCapacity(data.getCapacity());
        existing.setZone(data.getZone());
        existing.setStatus(data.getStatus());
        return tableRepository.save(existing);
    }

    @Override
    @Transactional
    public DiningTable updateStatus(Long id, TableStatus status) {
        if (status == null) {
            throw new BadRequestException("Trạng thái bàn không được để trống");
        }
        DiningTable existing = getById(id);
        existing.setStatus(status);
        return tableRepository.save(existing);
    }

    @Override
    @Transactional
    public DiningTable release(Long id) {
        DiningTable existing = getById(id);
        LocalDateTime now = LocalDateTime.now();
        boolean held = !reservationRepository.findByTableIdAndStatusAndReservedAtBetween(
                id, ReservationStatus.CONFIRMED, now.minusMinutes(30), now.plusMinutes(30)).isEmpty();
        existing.setStatus(held ? TableStatus.RESERVED : TableStatus.AVAILABLE);
        return tableRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DiningTable existing = getById(id);
        long active = reservationRepository.findByTableId(id).stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING || r.getStatus() == ReservationStatus.CONFIRMED)
                .count();
        if (active > 0) {
            throw new BadRequestException("Bàn còn " + active + " lượt đặt đang chờ / đã xác nhận. Hãy hủy hoặc chuyển các lượt đó sang bàn khác trước");
        }
        existing.setDeleted(true);
        existing.setDeletedAt(LocalDateTime.now());
        tableRepository.save(existing);
    }

    @Override
    @Transactional
    public DiningTable restore(Long id) {
        DiningTable existing = getById(id);
        existing.setDeleted(false);
        existing.setDeletedAt(null);
        return tableRepository.save(existing);
    }

    @Override
    @Transactional
    public void purge(Long id) {
        DiningTable existing = getById(id);
        if (!Boolean.TRUE.equals(existing.getDeleted())) {
            throw new BadRequestException("Chỉ xóa vĩnh viễn được bàn đang trong thùng rác");
        }
        tableRepository.delete(existing);
    }

    private void validate(DiningTable table) {
        if (table == null || table.getTableCode() == null || table.getTableCode().isBlank()) {
            throw new BadRequestException("Mã bàn không được để trống");
        }
        if (table.getCapacity() == null || table.getCapacity() <= 0) {
            throw new BadRequestException("Sức chứa phải lớn hơn 0");
        }
        if (table.getZone() == null || table.getZone().isBlank()) {
            throw new BadRequestException("Khu vực không được để trống");
        }
    }
}
