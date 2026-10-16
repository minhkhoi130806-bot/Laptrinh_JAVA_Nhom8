package com.restaurant.backend.controller;

import com.restaurant.backend.dto.OrderRequest;
import com.restaurant.backend.dto.OrderResponse;
import com.restaurant.backend.dto.OrderStatusRequest;
import com.restaurant.backend.dto.OrderTotals;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.mapper.OrderMapper;
import com.restaurant.backend.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
        return toResponse(orderService.create(orderMapper.toEntity(request)));
    }

    // Lay danh sach don hang: GET /api/orders?customerId=1&status=PAID
    @GetMapping
    public List<OrderResponse> getAll(
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) String status) {
        List<Order> orders = orderService.getAll(customerId, status);
        Map<Integer, OrderTotals> totals = orderService.totals(
                orders.stream().map(Order::getId).toList());
        return orders.stream()
                .map(o -> orderMapper.toResponse(o, totals.get(o.getId())))
                .toList();
    }

    // Tim don hang theo ID
    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Integer id) {
        return toResponse(orderService.getById(id));
    }

    // Cap nhat don hang
    @PutMapping("/{id}")
    public OrderResponse update(
            @PathVariable Integer id,
            @RequestBody OrderRequest request) {
        return toResponse(orderService.update(id, orderMapper.toEntity(request)));
    }

    // Doi trang thai: PATCH /api/orders/{id}/status  body: {"status":"PAID"}
    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(
            @PathVariable Integer id,
            @RequestBody OrderStatusRequest request) {
        return toResponse(orderService.updateStatus(id, request.status()));
    }

    // Xoa don hang
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        orderService.delete(id);
    }

    private OrderResponse toResponse(Order order) {
        Map<Integer, OrderTotals> totals = orderService.totals(List.of(order.getId()));
        return orderMapper.toResponse(order, totals.get(order.getId()));
    }
}
