
package com.restaurant.backend.controller;

import com.restaurant.backend.dto.OrderItemRequest;
import com.restaurant.backend.dto.OrderItemResponse;
import com.restaurant.backend.mapper.OrderItemMapper;
import com.restaurant.backend.service.OrderItemService;
import com.restaurant.backend.entity.OrderItemId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Quan ly API chi tiet don hang
@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

    private final OrderItemService orderItemService;
    private final OrderItemMapper orderItemMapper;

    public OrderItemController(
            OrderItemService orderItemService,
            OrderItemMapper orderItemMapper) {
        this.orderItemService = orderItemService;
        this.orderItemMapper = orderItemMapper;
    }

    // Them chi tiet don hang
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderItemResponse create(
            @RequestBody OrderItemRequest request) {
        return orderItemMapper.toResponse(
                orderItemService.create(orderItemMapper.toEntity(request))
        );
    }

    // Lay danh sach chi tiet don hang
    @GetMapping
    public List<OrderItemResponse> getAll() {
        return orderItemService.getAll()
                .stream()
                .map(orderItemMapper::toResponse)
                .toList();
    }

    // Tim chi tiet theo khoa chinh kep
    @GetMapping("/{orderId}/{menuItemId}")
    public OrderItemResponse getById(
            @PathVariable Integer orderId,
            @PathVariable Integer menuItemId) {

        OrderItemId id = new OrderItemId(orderId, menuItemId);
        return orderItemMapper.toResponse(
                orderItemService.getById(id)
        );
    }

    // Cap nhat chi tiet don hang
    @PutMapping("/{orderId}/{menuItemId}")
    public OrderItemResponse update(
            @PathVariable Integer orderId,
            @PathVariable Integer menuItemId,
            @RequestBody OrderItemRequest request) {

        OrderItemId id = new OrderItemId(orderId, menuItemId);
        return orderItemMapper.toResponse(
                orderItemService.update(
                        id,
                        orderItemMapper.toEntity(request)
                )
        );
    }

    // Xoa chi tiet don hang
    @DeleteMapping("/{orderId}/{menuItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Integer orderId,
            @PathVariable Integer menuItemId) {

        OrderItemId id = new OrderItemId(orderId, menuItemId);
        orderItemService.delete(id);
    }
}
