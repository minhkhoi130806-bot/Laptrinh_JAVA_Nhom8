package com.restaurant.backend.controller;

import com.restaurant.backend.dto.MomoCreateRequest;
import com.restaurant.backend.dto.MomoCreateResponse;
import com.restaurant.backend.dto.MomoStatusResponse;
import com.restaurant.backend.dto.VnpayCreateRequest;
import com.restaurant.backend.dto.VnpayCreateResponse;
import com.restaurant.backend.dto.VnpayStatusResponse;
import com.restaurant.backend.payment.MomoPaymentService;
import com.restaurant.backend.payment.VnpayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/** API thanh toán online. Các endpoint này công khai (khách chưa đăng nhập, MoMo/VNPay gọi IPN); bảo vệ bằng xác thực số điện thoại, chữ ký. */
@RestController
@RequestMapping("/api/payments")
public class MomoPaymentController {
    private final MomoPaymentService momo;
    private final VnpayPaymentService vnpay;

    public MomoPaymentController(MomoPaymentService momo, VnpayPaymentService vnpay) { this.momo = momo; this.vnpay = vnpay; }

    /** Giao diện dùng để ẩn/hiện phương thức online theo cấu hình máy chủ. */
    @GetMapping("/methods")
    public Map<String, Object> methods() {
        Map<String, Object> result = new HashMap<>();
        result.put("momo", momo.isEnabled());
        result.put("vnpay", vnpay.isEnabled());
        result.put("missing", momo.missingConfig());
        result.put("vnpayMissing", vnpay.missingConfig());
        return result;
    }

    // ======================== MOMO ========================

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

    // ======================== VNPAY ========================

    /** Tạo URL thanh toán VNPay và trả về cho frontend redirect. */
    @PostMapping("/vnpay/create")
    public VnpayCreateResponse vnpayCreate(@Valid @RequestBody VnpayCreateRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = httpRequest.getRemoteAddr();
        } else {
            // X-Forwarded-For có thể chứa nhiều IP, lấy IP đầu tiên
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return vnpay.createPayment(request.orderId(), request.phone(), ipAddress);
    }

    /**
     * VNPay gọi IPN (server-to-server) qua GET.
     * VNPay yêu cầu phản hồi JSON với RspCode.
     */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<String> vnpayIpn(@RequestParam Map<String, String> params) {
        String result = vnpay.handleIpn(params);
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(result);
    }

    /**
     * Xử lý return URL: VNPay redirect khách về đây.
     * Frontend gọi endpoint này để xác nhận trạng thái thanh toán.
     */
    @GetMapping("/vnpay/return")
    public VnpayStatusResponse vnpayReturn(@RequestParam Map<String, String> params) {
        return vnpay.handleReturn(params);
    }

    /** Truy vấn trạng thái giao dịch VNPay (cho trang kết quả). */
    @GetMapping("/vnpay/status")
    public VnpayStatusResponse vnpayStatus(@RequestParam String txnRef) {
        return vnpay.getStatus(txnRef);
    }
}
