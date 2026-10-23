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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * =========================================================================
 * CONTROLLER QUẢN LÝ ĐƠN HÀNG (/api/orders)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Đơn hàng)
 * 
 * Mục đích:
 * Tiếp nhận các request HTTP từ Client liên quan đến Đơn hàng (Order):
 * 1. POST   /api/orders            : Tạo đơn hàng mới
 * 2. GET    /api/orders            : Lấy danh sách tất cả đơn hàng (hoặc lọc theo khách/trạng thái)
 * 3. GET    /api/orders/{id}       : Lấy thông tin chi tiết một đơn hàng theo ID
 * 4. PUT    /api/orders/{id}       : Cập nhật thông tin đơn hàng
 * 5. PATCH  /api/orders/{id}/status : Thay đổi trạng thái đơn hàng (ví dụ: sang SERVING hoặc PAID)
 * 6. DELETE /api/orders/{id}       : Xóa đơn hàng
 * =========================================================================
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    // Tiêm Service và Mapper qua Constructor
    public OrderController(OrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    /**
     * API 1: Tạo đơn hàng mới
     * Endpoint: POST /api/orders
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@RequestBody OrderRequest request) {
        // Chuyển DTO request từ client thành Entity
        Order entity = orderMapper.toEntity(request);

        // Gọi tầng Service để thực hiện lưu đơn hàng
        Order donHangMoi = orderService.create(entity);

        // Chuyển kết quả Entity thành DTO Response trả về
        return chuyenThanhResponse(donHangMoi);
    }

    /**
     * API 2: Lấy danh sách đơn hàng
     * Endpoint: GET /api/orders?customerId=1&status=OPEN
     */
    @GetMapping
    public List<OrderResponse> getAll(
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) String status) {

        // Lấy danh sách đơn hàng từ CSDL
        List<Order> danhSachDon = orderService.getAll(customerId, status);

        // Lấy danh sách ID của các đơn hàng để tính tổng tiền
        List<Integer> danhSachId = new ArrayList<>();
        for (Order don : danhSachDon) {
            danhSachId.add(don.getId());
        }

        // Tính tổng tiền và tổng số món cho từng đơn hàng
        Map<Integer, OrderTotals> mapTongTien = orderService.totals(danhSachId);

        // Ghép thông tin đơn hàng và tổng tiền thành danh sách Response
        List<OrderResponse> ketQua = new ArrayList<>();
        for (Order don : danhSachDon) {
            OrderTotals totals = mapTongTien.get(don.getId());
            ketQua.add(orderMapper.toResponse(don, totals));
        }

        return ketQua;
    }

    /**
     * API 3: Tìm đơn hàng theo ID
     * Endpoint: GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Integer id) {
        Order donHang = orderService.getById(id);
        return chuyenThanhResponse(donHang);
    }

    /**
     * API 4: Cập nhật đơn hàng
     * Endpoint: PUT /api/orders/{id}
     */
    @PutMapping("/{id}")
    public OrderResponse update(
            @PathVariable Integer id,
            @RequestBody OrderRequest request) {

        Order entity = orderMapper.toEntity(request);
        Order donHangCapNhat = orderService.update(id, entity);
        return chuyenThanhResponse(donHangCapNhat);
    }

    /**
     * API 5: Thay đổi trạng thái đơn hàng
     * Endpoint: PATCH /api/orders/{id}/status
     * Body: { "status": "SERVING" }
     */
    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(
            @PathVariable Integer id,
            @RequestBody OrderStatusRequest request) {

        Order donHang = orderService.updateStatus(id, request.status());
        return chuyenThanhResponse(donHang);
    }

    /**
     * API 6: Xóa đơn hàng
     * Endpoint: DELETE /api/orders/{id}
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        orderService.delete(id);
    }

    /**
     * Hàm phụ trợ chuyển đổi Entity đơn hàng sang DTO kèm tổng tiền để trả về cho Client
     */
    private OrderResponse chuyenThanhResponse(Order order) {
        Map<Integer, OrderTotals> mapTong = orderService.totals(List.of(order.getId()));
        OrderTotals totals = mapTong.get(order.getId());
        return orderMapper.toResponse(order, totals);
    }
}
