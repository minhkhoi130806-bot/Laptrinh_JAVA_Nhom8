package com.restaurant.backend.controller;

import com.restaurant.backend.dto.MomoCreateRequest;
import com.restaurant.backend.dto.MomoCreateResponse;
import com.restaurant.backend.dto.MomoStatusResponse;
import com.restaurant.backend.payment.MomoPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** API thanh toán online. Các endpoint này công khai (khách chưa đăng nhập, MoMo gọi IPN); bảo vệ bằng xác thực số điện thoại, chữ ký và truy vấn ngược MoMo. */
@RestController
@RequestMapping("/api/payments")
public class MomoPaymentController {
    private final MomoPaymentService momo;

    public MomoPaymentController(MomoPaymentService momo) { this.momo = momo; }

    /** Giao diện dùng để ẩn/hiện phương thức online theo cấu hình máy chủ. */
    @GetMapping("/methods")
    public Map<String, Object> methods() { return Map.of("momo", momo.isEnabled(), "missing", momo.missingConfig()); }

    @PostMapping("/momo/create")
    public MomoCreateResponse create(@Valid @RequestBody MomoCreateRequest request) {
        return momo.createPayment(request.orderId(), request.phone());
    }

    /** MoMo gọi server-to-server; phản hồi 204 nghĩa là đã nhận. Chữ ký sai trả 400. */
    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> ipn(@RequestBody Map<String, Object> body) {
        momo.handleIpn(body);
        return ResponseEntity.noContent().build();
    }

    /** Trang kết quả hỏi trạng thái; backend truy vấn MoMo chứ không tin tham số trên URL. */
    @GetMapping("/momo/status")
    public MomoStatusResponse status(@RequestParam String orderId) {
        return momo.refresh(orderId);
    }
}
