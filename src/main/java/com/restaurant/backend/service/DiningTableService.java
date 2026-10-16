package com.restaurant.backend.service;

import com.restaurant.backend.dto.ListScope;
import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.TableStatus;

import java.util.List;

public interface DiningTableService {
    DiningTable create(DiningTable table);
    List<DiningTable> getAll(TableStatus status, ListScope scope);
    DiningTable getById(Long id);
    DiningTable update(Long id, DiningTable table);
    DiningTable updateStatus(Long id, TableStatus status);
    // Trả bàn: về "Đã đặt" nếu sắp có khách đã xác nhận, ngược lại về "Trống"
    DiningTable release(Long id);
    void delete(Long id);           // chuyển vào thùng rác
    DiningTable restore(Long id);   // khôi phục từ thùng rác
    void purge(Long id);            // xóa vĩnh viễn
}
