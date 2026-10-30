package com.restaurant.backend.payment;

import com.restaurant.backend.dto.CheckoutRequest;
import com.restaurant.backend.dto.VnpayCreateResponse;
import com.restaurant.backend.dto.VnpayStatusResponse;
import com.restaurant.backend.dto.OrderTotals;
import com.restaurant.backend.entity.Invoice;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.entity.PaymentTransaction;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.exception.PaymentGatewayException;
import com.restaurant.backend.repository.PaymentTransactionRepository;
import com.restaurant.backend.service.InvoiceService;
import com.restaurant.backend.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Nghiệp vụ thanh toán VNPay.
 * Nguyên tắc: số tiền do server tính từ DB; chỉ tin chữ ký return/IPN;
 * xử lý thành công đúng một lần nhờ khóa dòng giao dịch.
 */
@Service
public class VnpayPaymentService {
    private static final Logger log = LoggerFactory.getLogger(VnpayPaymentService.class);

    public static final String PROVIDER = "VNPAY";
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String EXPIRED = "EXPIRED";

    // VNPay sandbox: giới hạn tương tự MoMo
    private static final long MIN_AMOUNT = 5_000L;
    private static final long MAX_AMOUNT = 1_000_000_000L;

    private final VnpayProperties props;
    private final VnpayClient client;
    private final PaymentTransactionRepository repo;
    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final TransactionTemplate tx;

    public VnpayPaymentService(VnpayProperties props, VnpayClient client, PaymentTransactionRepository repo,
                               OrderService orderService, InvoiceService invoiceService, PlatformTransactionManager txManager) {
        this.props = props;
        this.client = client;
        this.repo = repo;
        this.orderService = orderService;
        this.invoiceService = invoiceService;
        this.tx = new TransactionTemplate(txManager);
    }

    public boolean isEnabled() { return props.isConfigured(); }

    public List<String> missingConfig() { return props.missing(); }

    // ------------------------------------------------------------------ tạo giao dịch

    public VnpayCreateResponse createPayment(Integer orderId, String phone, String ipAddress) {
        requireConfigured();
        Order order = loadOwnedOrder(orderId, phone);
        if (!"VNPAY".equalsIgnoreCase(order.getPaymentPreference())) {
            throw new BadRequestException("Đơn hàng này không chọn thanh toán bằng VNPay");
        }
        // Nếu lần thanh toán trước thực ra đã thành công thì không thu tiền hai lần.
        if (!repo.findByOrderIdAndProviderAndStatus(orderId.longValue(), PROVIDER, SUCCESS).isEmpty()) {
            throw new BadRequestException("Đơn hàng này đã được thanh toán bằng VNPay");
        }
        assertPayable(order);

        BigDecimal total = serverTotal(orderId);
        long amount = toWholeVnd(total);
        String vnpTxnRef = "HS" + orderId + "V" + System.currentTimeMillis();

        tx.execute(status -> {
            PaymentTransaction t = new PaymentTransaction();
            t.setOrderId(orderId.longValue());
            t.setProvider(PROVIDER);
            t.setIdempotencyKey(vnpTxnRef);
            t.setAmount(BigDecimal.valueOf(amount));
            t.setStatus(PENDING);
            return repo.save(t);
        });
        log.info("VNPAY create: vnpTxnRef={} orderId={} amount={}", vnpTxnRef, orderId, amount);

        String payUrl;
        try {
            payUrl = client.createPayUrl(vnpTxnRef, amount,
                    "Thanh toan don hang #" + orderId + " - Han Seoul", ipAddress);
        } catch (RuntimeException e) {
            markFailed(vnpTxnRef);
            throw e;
        }
        return new VnpayCreateResponse(vnpTxnRef, payUrl, BigDecimal.valueOf(amount));
    }

    // ------------------------------------------------------------------ xử lý return URL / IPN

    /**
     * Xử lý callback từ VNPay (cả return URL lẫn IPN đều gửi các tham số vnp_ qua query string).
     * Return URL: GET từ trình duyệt của khách; IPN: GET từ server VNPay.
     * Chữ ký sai trả 400.
     */
    public VnpayStatusResponse handleReturn(Map<String, String> params) {
        requireConfigured();

        // Xác thực chữ ký
        if (!VnpaySignature.verify(props.getHashSecret(), params)) {
            log.warn("VNPAY chữ ký không hợp lệ: vnp_TxnRef={}", params.get("vnp_TxnRef"));
            throw new BadRequestException("Chữ ký VNPay không hợp lệ");
        }

        String vnpTxnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        // VNPay trả amount * 100
        long amount = 0;
        try { amount = Long.parseLong(params.getOrDefault("vnp_Amount", "0")) / 100; } catch (NumberFormatException ignored) {}

        log.info("VNPAY return: vnpTxnRef={} responseCode={} transactionNo={} amount={} bankCode={}",
                vnpTxnRef, responseCode, transactionNo, amount, bankCode);

        apply(vnpTxnRef, responseCode, amount, transactionNo, "RETURN");

        PaymentTransaction t = repo.findByIdempotencyKey(vnpTxnRef)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch"));
        return toStatus(t);
    }

    /**
     * Xử lý IPN từ VNPay. Trả "00" nếu thành công, "01" nếu lỗi.
     * VNPay IPN gọi GET với cùng tham số như return URL.
     */
    public String handleIpn(Map<String, String> params) {
        requireConfigured();

        if (!VnpaySignature.verify(props.getHashSecret(), params)) {
            log.warn("VNPAY IPN sai chữ ký: vnp_TxnRef={}", params.get("vnp_TxnRef"));
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}";
        }

        String vnpTxnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        long amount = 0;
        try { amount = Long.parseLong(params.getOrDefault("vnp_Amount", "0")) / 100; } catch (NumberFormatException ignored) {}

        PaymentTransaction t = repo.findByIdempotencyKey(vnpTxnRef).orElse(null);
        if (t == null) {
            log.warn("VNPAY IPN: không tìm thấy giao dịch {}", vnpTxnRef);
            return "{\"RspCode\":\"01\",\"Message\":\"Order not Found\"}";
        }

        if (SUCCESS.equals(t.getStatus())) {
            log.info("VNPAY IPN: bỏ qua bản lặp của giao dịch {}", vnpTxnRef);
            return "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}";
        }

        if (amount > 0 && t.getAmount().compareTo(BigDecimal.valueOf(amount)) != 0) {
            log.error("VNPAY IPN: SỐ TIỀN LỆCH vnpTxnRef={} expected={} actual={}", vnpTxnRef, t.getAmount(), amount);
            return "{\"RspCode\":\"04\",\"Message\":\"Invalid Amount\"}";
        }

        log.info("VNPAY IPN: vnpTxnRef={} responseCode={} transactionNo={} amount={}", vnpTxnRef, responseCode, transactionNo, amount);
        apply(vnpTxnRef, responseCode, amount, transactionNo, "IPN");

        return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";
    }

    // ------------------------------------------------------------------ truy vấn trạng thái (cho frontend)

    public VnpayStatusResponse getStatus(String vnpTxnRef) {
        PaymentTransaction t = repo.findByIdempotencyKey(vnpTxnRef)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch"));
        return toStatus(t);
    }

    // ------------------------------------------------------------------ đối soát định kỳ

    /** Gọi từ VnpayReconcileJob: đánh dấu hết hạn giao dịch treo, hoàn tất đơn bị dang dở. */
    public void reconcile() {
        if (!isEnabled()) return;
        LocalDateTime now = LocalDateTime.now();
        // Hết hạn các giao dịch PENDING quá 30 phút
        List<PaymentTransaction> pending = repo.findByProviderAndStatusAndCreatedAtBetween(
                PROVIDER, PENDING, now.minusDays(2), now.minusMinutes(3));
        for (PaymentTransaction t : pending) {
            try {
                if (PENDING.equals(t.getStatus()) && t.getCreatedAt().isBefore(now.minusMinutes(30))) {
                    expire(t.getIdempotencyKey());
                }
            } catch (RuntimeException e) {
                log.warn("VNPAY reconcile lỗi {}: {}", t.getIdempotencyKey(), e.getMessage());
            }
        }
        // Hoàn tất đơn đã SUCCESS nhưng chưa có invoice
        for (PaymentTransaction t : repo.findByProviderAndStatusAndInvoiceIdIsNullAndCreatedAtAfter(
                PROVIDER, SUCCESS, now.minusDays(7))) {
            try { finalizeOrder(t.getIdempotencyKey()); }
            catch (RuntimeException e) { log.error("VNPAY không hoàn tất đơn cho giao dịch {}: {}", t.getIdempotencyKey(), e.getMessage()); }
        }
    }

    // ------------------------------------------------------------------ nội bộ

    /** Áp kết quả từ VNPay: idempotent, kiểm tra số tiền, không bao giờ hạ SUCCESS. */
    private void apply(String vnpTxnRef, String responseCode, long amount, String transactionNo, String source) {
        Boolean newlySucceeded = tx.execute(status -> {
            PaymentTransaction t = repo.findWithLockByIdempotencyKey(vnpTxnRef).orElse(null);
            if (t == null) {
                log.warn("VNPAY {}: không có giao dịch {}", source, vnpTxnRef);
                return Boolean.FALSE;
            }
            if (SUCCESS.equals(t.getStatus())) {
                log.info("VNPAY {}: bỏ qua bản lặp của giao dịch {}", source, vnpTxnRef);
                return Boolean.FALSE;
            }
            if (amount > 0 && t.getAmount().compareTo(BigDecimal.valueOf(amount)) != 0) {
                log.error("VNPAY {}: SỐ TIỀN LỆCH vnpTxnRef={} expected={} actual={}", source, vnpTxnRef, t.getAmount(), amount);
                throw new BadRequestException("Số tiền không khớp");
            }
            // VNPay responseCode "00" = thành công
            if ("00".equals(responseCode)) {
                t.setStatus(SUCCESS);
                t.setPaidAt(LocalDateTime.now());
                if (transactionNo != null && !transactionNo.isBlank()) {
                    t.setProviderTransactionId(transactionNo);
                }
                repo.save(t);
                log.info("VNPAY {}: THÀNH CÔNG vnpTxnRef={} transactionNo={}", source, vnpTxnRef, transactionNo);
                return Boolean.TRUE;
            }
            // Các mã lỗi khác -> FAILED
            if (PENDING.equals(t.getStatus())) {
                t.setStatus(FAILED);
                repo.save(t);
                log.info("VNPAY {}: THẤT BẠI vnpTxnRef={} responseCode={}", source, vnpTxnRef, responseCode);
            }
            return Boolean.FALSE;
        });
        if (Boolean.TRUE.equals(newlySucceeded)) {
            try { finalizeOrder(vnpTxnRef); }
            catch (RuntimeException e) { log.error("VNPAY đã nhận tiền nhưng chưa hoàn tất đơn: vnpTxnRef={} lỗi={}", vnpTxnRef, e.getMessage()); }
        }
    }

    /** Tạo hóa đơn + chuyển đơn sang PAID (tái dùng InvoiceService.checkout). */
    private void finalizeOrder(String vnpTxnRef) {
        tx.executeWithoutResult(status -> {
            PaymentTransaction t = repo.findWithLockByIdempotencyKey(vnpTxnRef).orElse(null);
            if (t == null || !SUCCESS.equals(t.getStatus()) || t.getInvoiceId() != null || t.getOrderId() == null) return;
            Order order = orderService.getById(t.getOrderId().intValue());
            if ("PAID".equalsIgnoreCase(order.getStatus())) {
                log.warn("VNPAY ĐỐI SOÁT: đơn #{} đã PAID từ trước, giao dịch {} cần kiểm tra/hoàn tiền", order.getId(), vnpTxnRef);
                return;
            }
            if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
                log.warn("VNPAY ĐỐI SOÁT: đơn #{} đã hủy nhưng khách đã trả tiền (giao dịch {}), cần hoàn tiền", order.getId(), vnpTxnRef);
                return;
            }
            if (serverTotal(order.getId()).compareTo(t.getAmount()) != 0) {
                log.warn("VNPAY ĐỐI SOÁT: tổng đơn #{} đã thay đổi sau khi tạo giao dịch {}, cần xử lý thủ công", order.getId(), vnpTxnRef);
                return;
            }
            Invoice invoice = invoiceService.checkout(new CheckoutRequest(order.getId(), null, "VNPAY"));
            t.setInvoiceId(invoice.getId().longValue());
            repo.save(t);
        });
    }

    private void markFailed(String vnpTxnRef) {
        tx.executeWithoutResult(status -> repo.findWithLockByIdempotencyKey(vnpTxnRef).ifPresent(t -> {
            if (PENDING.equals(t.getStatus())) { t.setStatus(FAILED); repo.save(t); }
        }));
    }

    private void expire(String vnpTxnRef) {
        tx.executeWithoutResult(status -> repo.findWithLockByIdempotencyKey(vnpTxnRef).ifPresent(t -> {
            if (PENDING.equals(t.getStatus())) { t.setStatus(EXPIRED); repo.save(t); log.info("VNPAY hết hạn: {}", vnpTxnRef); }
        }));
    }

    private Order loadOwnedOrder(Integer orderId, String phone) {
        String notFound = "Không tìm thấy đơn hàng với mã và số điện thoại đã cung cấp";
        Order order;
        try { order = orderService.getById(orderId); } catch (NotFoundException e) { throw new BadRequestException(notFound); }
        if (phone == null || order.getCustomerPhone() == null || !order.getCustomerPhone().equals(phone.trim())) {
            throw new BadRequestException(notFound);
        }
        return order;
    }

    private static void assertPayable(Order order) {
        String st = order.getStatus() == null ? "" : order.getStatus().toUpperCase(Locale.ROOT);
        if ("PAID".equals(st)) throw new BadRequestException("Đơn hàng này đã được thanh toán");
        if ("CANCELLED".equals(st)) throw new BadRequestException("Đơn hàng đã bị hủy, không thể thanh toán");
    }

    private BigDecimal serverTotal(Integer orderId) {
        OrderTotals totals = orderService.totals(List.of(orderId)).getOrDefault(orderId, OrderTotals.EMPTY);
        return totals.total() == null ? BigDecimal.ZERO : totals.total();
    }

    private static long toWholeVnd(BigDecimal total) {
        BigDecimal t = total.stripTrailingZeros();
        if (t.scale() > 0) throw new BadRequestException("Tổng tiền đơn hàng không phải số nguyên VND, vui lòng liên hệ nhà hàng");
        long amount = t.longValueExact();
        if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            throw new BadRequestException("VNPay chỉ hỗ trợ số tiền từ 5.000 đến 1.000.000.000 ₫ cho mỗi giao dịch");
        }
        return amount;
    }

    private VnpayStatusResponse toStatus(PaymentTransaction t) {
        return new VnpayStatusResponse(t.getIdempotencyKey(), t.getStatus(),
                t.getOrderId() == null ? null : t.getOrderId().intValue(), t.getAmount(),
                t.getProviderTransactionId());
    }

    private void requireConfigured() {
        if (!props.isConfigured()) throw new PaymentGatewayException("Thanh toán VNPay chưa được cấu hình trên máy chủ");
    }
}
