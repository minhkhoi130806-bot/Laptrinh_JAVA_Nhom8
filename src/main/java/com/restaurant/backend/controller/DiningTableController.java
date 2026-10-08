package com.restaurant.backend.controller;

import com.restaurant.backend.dto.DiningTableRequest;
import com.restaurant.backend.dto.DiningTableResponse;
import com.restaurant.backend.entity.TableStatus;
import com.restaurant.backend.mapper.DiningTableMapper;
import com.restaurant.backend.service.DiningTableService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class DiningTableController {

    private final DiningTableService tableService;
    private final DiningTableMapper tableMapper;

    public DiningTableController(DiningTableService tableService, DiningTableMapper tableMapper) {
        this.tableService = tableService;
        this.tableMapper = tableMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiningTableResponse addTable(@RequestBody DiningTableRequest request) {
        return tableMapper.toResponse(tableService.create(tableMapper.toEntity(request)));
    }

    // GET /api/tables hoặc GET /api/tables?status=AVAILABLE
    @GetMapping
    public List<DiningTableResponse> getAllTables(@RequestParam(required = false) TableStatus status) {
        return tableService.getAll(status).stream().map(tableMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DiningTableResponse getTable(@PathVariable Long id) {
        return tableMapper.toResponse(tableService.getById(id));
    }

    @PutMapping("/{id}")
    public DiningTableResponse updateTable(@PathVariable Long id, @RequestBody DiningTableRequest request) {
        return tableMapper.toResponse(tableService.update(id, tableMapper.toEntity(request)));
    }

    // PATCH /api/tables/{id}/status?value=OCCUPIED  (dùng cho sơ đồ bàn)
    @PatchMapping("/{id}/status")
    public DiningTableResponse updateTableStatus(@PathVariable Long id, @RequestParam("value") TableStatus status) {
        return tableMapper.toResponse(tableService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTable(@PathVariable Long id) {
        tableService.delete(id);
    }
}
