package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.DiningTableRequest;
import com.restaurant.backend.dto.DiningTableResponse;
import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.TableStatus;
import org.springframework.stereotype.Component;

@Component
public class DiningTableMapper {

    public DiningTable toEntity(DiningTableRequest request) {
        if (request == null) {
            return null;
        }
        DiningTable table = new DiningTable();
        table.setTableCode(request.tableCode());
        table.setCapacity(request.capacity());
        table.setZone(request.zone());
        table.setStatus(request.status() != null ? request.status() : TableStatus.AVAILABLE);
        return table;
    }

    public DiningTableResponse toResponse(DiningTable table) {
        return new DiningTableResponse(
                table.getId(),
                table.getTableCode(),
                table.getCapacity(),
                table.getZone(),
                table.getStatus()
        );
    }
}
