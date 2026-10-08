package com.restaurant.backend.service.Impl;

import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.TableStatus;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.DiningTableRepository;
import com.restaurant.backend.service.DiningTableService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DiningTableServiceImpl implements DiningTableService {

    private final DiningTableRepository tableRepository;

    public DiningTableServiceImpl(DiningTableRepository tableRepository) {
        this.tableRepository = tableRepository;
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
    public List<DiningTable> getAll(TableStatus status) {
        return status == null ? tableRepository.findAll() : tableRepository.findByStatus(status);
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
    public void delete(Long id) {
        tableRepository.delete(getById(id));
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
