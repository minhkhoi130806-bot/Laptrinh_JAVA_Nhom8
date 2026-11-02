package com.restaurant.backend.controller;

import com.restaurant.backend.dto.*;
import com.restaurant.backend.entity.Customer;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.entity.OrderItem;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.mapper.OrderItemMapper;
import com.restaurant.backend.repository.CustomerRepository;
import com.restaurant.backend.service.OrderItemService;
import com.restaurant.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** API riêng cho khách đặt món; không mở quyền truy cập trực tiếp các API quản trị đơn hàng. */
@RestController
@RequestMapping("/api/public-orders")
public class PublicOrderController {
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final OrderItemMapper orderItemMapper;
    private final CustomerRepository customerRepository;

    public PublicOrderController(
            OrderService orderService,
            OrderItemService orderItemService,
            OrderItemMapper orderItemMapper,
            CustomerRepository customerRepository) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
        this.orderItemMapper = orderItemMapper;
        this.customerRepository = customerRepository;
    }

    @PostMapping
    @Transactional
    public PublicOrderCreatedResponse create(@Valid @RequestBody PublicOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) throw new BadRequestException("Giỏ hàng đang trống");
        if (request.items().size() > 50) throw new BadRequestException("Mỗi đơn chỉ được có tối đa 50 dòng món");
        Order order = new Order();
        String name = request.customerName() != null ? request.customerName().trim() : "";
        String phone = request.customerPhone() != null ? request.customerPhone().trim() : "";
        String email = request.customerEmail() != null ? request.customerEmail().trim() : null;

        order.setCustomerName(name);
        order.setCustomerPhone(phone);
        order.setCustomerEmail(email);
        order.setOrderType(request.orderType());
        order.setDeliveryAddress(request.deliveryAddress());
        order.setCustomerNote(request.customerNote());
        order.setPaymentPreference(request.paymentPreference());
        order.setStatus("OPEN");

        // Tự động nhận diện tài khoản hội viên nếu khách đã đăng ký thành viên
        // Gắn customerId trực tiếp để đơn hàng không hiển thị "Khách vãng lai" trong hệ thống quản trị
        if (customerRepository != null && !phone.isBlank()) {
            Optional<Customer> found = customerRepository.findByPhone(phone);
            if (found.isEmpty() && email != null && !email.isBlank()) {
                found = customerRepository.findByEmail(email);
            }
            if (found.isPresent()) {
                Customer c = found.get();
                order.setCustomerId(c.getId().intValue());
                if (order.getCustomerName() == null || order.getCustomerName().isBlank()) {
                    order.setCustomerName(c.getFullName());
                }
                if (order.getCustomerEmail() == null && c.getEmail() != null) {
                    order.setCustomerEmail(c.getEmail());
                }
            }
        }

        Order saved = orderService.create(order);
        List<OrderItemResponse> items = new ArrayList<>();
        for (PublicOrderItemRequest item : request.items()) {
            // Không nhận đơn giá từ client; service lấy giá hiện tại của món trong database.
            OrderItem created = orderItemService.create(orderItemMapper.toEntity(new OrderItemRequest(saved.getId(), item.menuItemId(), item.quantity(), null, item.note())));
            items.add(orderItemMapper.toResponse(created));
        }
        Map<Integer, OrderTotals> totals = orderService.totals(List.of(saved.getId()));
        OrderTotals amount = totals.getOrDefault(saved.getId(), OrderTotals.EMPTY);
        return new PublicOrderCreatedResponse(saved.getId(), saved.getStatus(), saved.getCreatedAt(), amount.total(), saved.getCustomerName(), saved.getCustomerPhone(), saved.getCustomerEmail(), saved.getOrderType(), saved.getPaymentPreference(), items);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public PublicOrderCreatedResponse lookup(@PathVariable Integer id, @RequestParam String phone) {
        Order order = orderService.getById(id);
        if (order.getCustomerPhone() == null || !order.getCustomerPhone().equals(phone.trim())) {
            // Không tiết lộ đơn có tồn tại nếu số điện thoại không khớp.
            throw new BadRequestException("Không tìm thấy đơn hàng với mã và số điện thoại đã cung cấp");
        }
        List<OrderItemResponse> items = orderItemService.getByOrderId(id).stream().map(orderItemMapper::toResponse).toList();
        Map<Integer, OrderTotals> totals = orderService.totals(List.of(id));
        OrderTotals amount = totals.getOrDefault(id, OrderTotals.EMPTY);
        return new PublicOrderCreatedResponse(order.getId(), order.getStatus(), order.getCreatedAt(), amount.total(), order.getCustomerName(), order.getCustomerPhone(), order.getCustomerEmail(), order.getOrderType(), order.getPaymentPreference(), items);
    }
}
