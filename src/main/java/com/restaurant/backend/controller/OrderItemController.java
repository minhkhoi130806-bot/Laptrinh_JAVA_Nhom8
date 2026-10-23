package com.restaurant.backend.controller;

import com.restaurant.backend.dto.OrderItemRequest;
import com.restaurant.backend.dto.OrderItemResponse;
import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.entity.OrderItemId;
import com.restaurant.backend.mapper.OrderItemMapper;
import com.restaurant.backend.service.OrderItemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================
 * CONTROLLER GỌI MÓN / CHI TIẾT ĐƠN HÀNG (/api/order-items)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Gọi món & Chi tiết đơn)
 * 
 * Mục đích:
 * Tiếp nhận các request HTTP thêm, sửa, xóa món ăn trong đơn hàng của khách:
 * 1. POST   /api/order-items                     : Thêm món vào đơn (lưu unit_price tại thời điểm gọi)
 * 2. GET    /api/order-items?orderId=1           : Lấy danh sách các món trong đơn hàng số 1
 * 3. GET    /api/order-items/{orderId}/{menuItemId} : Xem chi tiết một món ăn trong đơn hàng
 * 4. PUT    /api/order-items/{orderId}/{menuItemId} : Sửa số lượng hoặc ghi chú của món
 * 5. DELETE /api/order-items/{orderId}/{menuItemId} : Xóa món ăn khỏi đơn
 * =========================================================================
 */
@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

    private final OrderItemService orderItemService;
    private final OrderItemMapper orderItemMapper;

    // Tiêm Service và Mapper qua Constructor
    public OrderItemController(
            OrderItemService orderItemService,
            OrderItemMapper orderItemMapper) {
        this.orderItemService = orderItemService;
        this.orderItemMapper = orderItemMapper;
    }

    /**
     * API 1: Thêm món ăn vào đơn hàng (Gọi món)
     * Endpoint: POST /api/order-items
     * Body: { "orderId": 1, "menuItemId": 5, "quantity": 2, "note": "Không cay" }
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderItemResponse create(@RequestBody OrderItemRequest request) {
        // Chuyển DTO request từ client sang Entity
        OrderItem entity = orderItemMapper.toEntity(request);

        // Gọi tầng Service để xử lý (tự động lưu unit_price lúc gọi món)
        OrderItem ketQua = orderItemService.create(entity);

        // Chuyển kết quả Entity thành DTO Response để trả về cho Client
        return orderItemMapper.toResponse(ketQua);
    }

    /**
     * API 2: Lấy danh sách các món ăn
     * Endpoint: GET /api/order-items?orderId=1 (Xem các món của đơn hàng số 1)
     */
    @GetMapping
    public List<OrderItemResponse> getAll(@RequestParam(required = false) Integer orderId) {
        List<OrderItem> danhSachMon;

        // Nếu client truyền orderId, chỉ lấy các món của đơn hàng đó
        if (orderId != null) {
            danhSachMon = orderItemService.getByOrderId(orderId);
        } else {
            // Ngược lại lấy tất cả các món của mọi đơn
            danhSachMon = orderItemService.getAll();
        }

        // Chuyển danh sách Entity thành danh sách DTO Response
        List<OrderItemResponse> ketQua = new ArrayList<>();
        for (OrderItem item : danhSachMon) {
            ketQua.add(orderItemMapper.toResponse(item));
        }

        return ketQua;
    }

    /**
     * API 3: Xem chi tiết 1 món ăn trong đơn theo khóa chính kép
     * Endpoint: GET /api/order-items/{orderId}/{menuItemId}
     */
    @GetMapping("/{orderId}/{menuItemId}")
    public OrderItemResponse getById(
            @PathVariable Integer orderId,
            @PathVariable Integer menuItemId) {

        OrderItemId id = new OrderItemId(orderId, menuItemId);
        OrderItem item = orderItemService.getById(id);
        return orderItemMapper.toResponse(item);
    }

    /**
     * API 4: Cập nhật số lượng hoặc ghi chú của món trong đơn
     * Endpoint: PUT /api/order-items/{orderId}/{menuItemId}
     */
    @PutMapping("/{orderId}/{menuItemId}")
    public OrderItemResponse update(
            @PathVariable Integer orderId,
            @PathVariable Integer menuItemId,
            @RequestBody OrderItemRequest request) {

        OrderItemId id = new OrderItemId(orderId, menuItemId);
        OrderItem entity = orderItemMapper.toEntity(request);

        OrderItem itemCapNhat = orderItemService.update(id, entity);
        return orderItemMapper.toResponse(itemCapNhat);
    }

    /**
     * API 5: Xóa món ăn khỏi đơn hàng (Hủy món)
     * Endpoint: DELETE /api/order-items/{orderId}/{menuItemId}
     */
    @DeleteMapping("/{orderId}/{menuItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Integer orderId,
            @PathVariable Integer menuItemId) {

        OrderItemId id = new OrderItemId(orderId, menuItemId);
        orderItemService.delete(id);
    }
}
