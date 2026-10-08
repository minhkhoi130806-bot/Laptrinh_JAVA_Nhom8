package com.restaurant.backend.service;

import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.TableStatus;

import java.util.List;

public interface DiningTableService {
    DiningTable create(DiningTable table);

    List<DiningTable> getAll(TableStatus status);

    DiningTable getById(Long id);

    DiningTable update(Long id, DiningTable table);

    DiningTable updateStatus(Long id, TableStatus status);

    void delete(Long id);
}
