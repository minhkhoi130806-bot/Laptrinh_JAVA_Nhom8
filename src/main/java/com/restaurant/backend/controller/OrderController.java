
package com.restaurant.backend.controller;

import com.restaurant.backend.dto.OrderRequest;
import com.restaurant.backend.dto.OrderResponse;
import com.restaurant.backend.mapper.OrderMapper;
import com.restaurant.backend.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Quan ly API don hang
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    public OrderController(
            OrderService orderService,
            OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    // Them don hang
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@RequestBody OrderRequest request) {
        return orderMapper.toResponse(
                orderService.create(orderMapper.toEntity(request))
        );
    }

    // Lay danh sach don hang
    @GetMapping
    public List<OrderResponse> getAll() {
        return orderService.getAll()
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    // Tim don hang theo ID
    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Integer id) {
        return orderMapper.toResponse(orderService.getById(id));
    }

    // Cap nhat don hang
    @PutMapping("/{id}")
    public OrderResponse update(
            @PathVariable Integer id,
            @RequestBody OrderRequest request) {
        return orderMapper.toResponse(
                orderService.update(id, orderMapper.toEntity(request))
        );
    }

    // Xoa don hang
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        orderService.delete(id);
    }
}

