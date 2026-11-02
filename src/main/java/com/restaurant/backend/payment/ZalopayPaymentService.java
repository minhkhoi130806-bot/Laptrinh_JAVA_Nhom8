package com.restaurant.backend.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.backend.dto.CheckoutRequest;
import com.restaurant.backend.dto.OrderTotals;
import com.restaurant.backend.dto.ZalopayCreateResponse;
import com.restaurant.backend.dto.ZalopayStatusResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Nghiệp vụ thanh toán ZaloPay (API v2).
 * Nguyên tắc: số tiền do server tính từ DB; chỉ tin chữ ký Callback hoặc kết quả truy vấn trực tiếp từ ZaloPay;
 * xử lý thành công đúng một lần nhờ khóa dòng giao dịch.
 */
@Service
public class ZalopayPaymentService {
    private static final Logger log = LoggerFactory.getLogger(ZalopayPaymentService.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.ofPattern("yyMMdd");

    public static final String PROVIDER = "ZALOPAY";
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String EXPIRED = "EXPIRED";

    // Giới hạn số tiền của ZaloPay sandbox
    private static final long MIN_AMOUNT = 1_000L;
    private static final long MAX_AMOUNT = 50_000_000L;

    private final ZalopayProperties props;
    private final ZalopayClient client;
    private final PaymentTransactionRepository repo;
    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final TransactionTemplate tx;

    public ZalopayPaymentService(ZalopayProperties props, ZalopayClient client, PaymentTransactionRepository repo,
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

    public ZalopayCreateResponse createPayment(Integer orderId, String phone) {
        requireConfigured();
        Order order = loadOwnedOrder(orderId, phone);
        if (!"ZALOPAY".equalsIgnoreCase(order.getPaymentPreference())) {
            throw new BadRequestException("Đơn hàng này không chọn thanh toán bằng ZaloPay");
        }
        // Nếu lần thanh toán trước thực ra đã thành công thì cập nhật trước, tránh thu tiền hai lần.
        for (PaymentTransaction old : repo.findByOrderIdAndProviderAndStatus(orderId.longValue(), PROVIDER, PENDING)) {
            try { refresh(old.getIdempotencyKey()); } catch (RuntimeException e) { log.warn("Không làm mới giao dịch cũ {}: {}", old.getIdempotencyKey(), e.getMessage()); }
        }
        order = loadOwnedOrder(orderId, phone);
        assertPayable(order);
        if (!repo.findByOrderIdAndProviderAndStatus(orderId.longValue(), PROVIDER, SUCCESS).isEmpty()) {
            throw new BadRequestException("Đơn hàng này đã được thanh toán bằng ZaloPay");
        }

        BigDecimal total = serverTotal(orderId);
        long amount = toWholeVnd(total);

        // ZaloPay quy định app_trans_id có format: yyMMdd_xxxx (tối đa 40 ký tự)
        String prefix = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).format(DATE_PREFIX);
        String appTransId = prefix + "_" + orderId + "T" + System.currentTimeMillis();

        PaymentTransaction created = tx.execute(status -> {
            PaymentTransaction t = new PaymentTransaction();
            t.setOrderId(orderId.longValue());
            t.setProvider(PROVIDER);
            t.setIdempotencyKey(appTransId);
            t.setAmount(BigDecimal.valueOf(amount));
            t.setStatus(PENDING);
            return repo.save(t);
        });
        log.info("ZALOPAY create: appTransId={} orderId={} amount={}", appTransId, orderId, amount);

        ZalopayGatewayResult result;
        try {
            result = client.create(appTransId, amount, "Thanh toan don hang #" + orderId + " - Han Seoul", order.getCustomerPhone());
        } catch (RuntimeException e) {
            markFailed(appTransId);
            throw e;
        }

        if (result.returnCode() != 1 || result.orderUrl() == null || result.orderUrl().isBlank()) {
            markFailed(appTransId);
            log.warn("ZALOPAY create bị từ chối: appTransId={} returnCode={} msg={}",
                    appTransId, result.returnCode(), result.returnMessage());
            throw new BadRequestException("ZaloPay từ chối tạo giao dịch" + (result.returnMessage() == null ? "" : ": " + result.returnMessage()));
        }

        return new ZalopayCreateResponse(created.getIdempotencyKey(), result.orderUrl(), BigDecimal.valueOf(amount));
    }

    // ------------------------------------------------------------------ Callback / IPN

    /**
     * Xử lý callback server-to-server từ ZaloPay.
     * ZaloPay gửi JSON { "data": "...", "mac": "...", "type": 1 }
     * Chữ ký mac được tính bằng HmacSHA256(key2, data).
     * Phản hồi lại ZaloPay: { "return_code": 1, "return_message": "success" }.
     */
    public Map<String, Object> handleCallback(Map<String, Object> body) {
        requireConfigured();
        Map<String, Object> response = new HashMap<>();

        String dataString = body.get("data") == null ? null : String.valueOf(body.get("data"));
        String requestMac = body.get("mac") == null ? null : String.valueOf(body.get("mac"));

        if (dataString == null || requestMac == null) {
            log.warn("ZALOPAY callback thiếu data hoặc mac");
            response.put("return_code", -1);
            response.put("return_message", "missing data or mac");
            return response;
        }

        if (!ZalopaySignature.verifyCallback(props.getKey2(), dataString, requestMac)) {
            log.warn("ZALOPAY callback sai chữ ký MAC");
            response.put("return_code", -1);
            response.put("return_message", "mac not equal");
            return response;
        }

        Map<String, Object> data;
        try {
            data = JSON.readValue(dataString, new TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            log.error("ZALOPAY callback không parse được data JSON: {}", e.getMessage());
            response.put("return_code", 0);
            response.put("return_message", "invalid json data");
            return response;
        }

        String appTransId = requireText(data.get("app_trans_id"), "app_trans_id");
        long amount = requireNumber(data.get("amount"), "amount");
        Long zpTransId = data.get("zp_trans_id") instanceof Number n ? n.longValue() : null;

        log.info("ZALOPAY callback: appTransId={} zpTransId={} amount={}", appTransId, zpTransId, amount);
        apply(appTransId, 1, amount, zpTransId, "CALLBACK");

        response.put("return_code", 1);
        response.put("return_message", "success");
        return response;
    }

    // ------------------------------------------------------------------ truy vấn trạng thái

    /** Hỏi trực tiếp ZaloPay rồi cập nhật; dùng cho trang kết quả và job đối soát. */
    public ZalopayStatusResponse refresh(String appTransId) {
        requireConfigured();
        PaymentTransaction t = repo.findByIdempotencyKey(appTransId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch ZaloPay với mã: " + appTransId));
        if (PENDING.equals(t.getStatus()) || EXPIRED.equals(t.getStatus())) {
            ZalopayGatewayResult r = client.query(appTransId);
            apply(appTransId, r.returnCode(), r.amount(), r.zpTransId(), "QUERY");
            t = repo.findByIdempotencyKey(appTransId).orElse(t);
        }
        return toStatus(t);
    }

    /** Lấy trạng thái giao dịch cho frontend trang kết quả. */
    public ZalopayStatusResponse getStatus(String appTransId) {
        requireConfigured();
        PaymentTransaction t = repo.findByIdempotencyKey(appTransId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch ZaloPay với mã: " + appTransId));
        if (PENDING.equals(t.getStatus())) {
            return refresh(appTransId);
        }
        return toStatus(t);
    }

    // ------------------------------------------------------------------ đối soát định kỳ

    /** Gọi từ ZalopayReconcileJob: làm mới giao dịch treo, đánh dấu hết hạn, hoàn tất đơn bị dang dở. */
    public void reconcile() {
        if (!isEnabled()) return;
        LocalDateTime now = LocalDateTime.now();
        List<PaymentTransaction> pending = repo.findByProviderAndStatusAndCreatedAtBetween(PROVIDER, PENDING, now.minusDays(2), now.minusMinutes(3));
        for (PaymentTransaction t : pending) {
            try {
                ZalopayStatusResponse s = refresh(t.getIdempotencyKey());
                if (PENDING.equals(s.status()) && t.getCreatedAt().isBefore(now.minusMinutes(30))) {
                    expire(t.getIdempotencyKey());
                }
            } catch (RuntimeException e) {
                log.warn("ZALOPAY reconcile lỗi {}: {}", t.getIdempotencyKey(), e.getMessage());
            }
        }
        for (PaymentTransaction t : repo.findByProviderAndStatusAndInvoiceIdIsNullAndCreatedAtAfter(PROVIDER, SUCCESS, now.minusDays(7))) {
            try { finalizeOrder(t.getIdempotencyKey()); } catch (RuntimeException e) { log.error("ZALOPAY không hoàn tất đơn cho giao dịch {}: {}", t.getIdempotencyKey(), e.getMessage()); }
        }
    }

    // ------------------------------------------------------------------ nội bộ

    /**
     * Áp kết quả từ ZaloPay vào giao dịch: idempotent, kiểm tra số tiền, không bao giờ hạ SUCCESS về trạng thái khác.
     * returnCode ZaloPay: 1 = Thành công, 2 = Thất bại, 3 = Đang xử lý
     */
    private void apply(String appTransId, int returnCode, Long amount, Long zpTransId, String source) {
        Boolean newlySucceeded = tx.execute(status -> {
            PaymentTransaction t = repo.findWithLockByIdempotencyKey(appTransId).orElse(null);
            if (t == null) {
                log.warn("ZALOPAY {}: không có giao dịch {}", source, appTransId);
                return Boolean.FALSE;
            }
            if (SUCCESS.equals(t.getStatus())) {
                log.info("ZALOPAY {}: bỏ qua bản lặp của giao dịch {}", source, appTransId);
                return Boolean.FALSE;
            }
            if (amount != null && t.getAmount().compareTo(BigDecimal.valueOf(amount)) != 0) {
                log.error("ZALOPAY {}: SỐ TIỀN LỆCH appTransId={} expected={} actual={}", source, appTransId, t.getAmount(), amount);
                throw new BadRequestException("Số tiền không khớp");
            }
            if (returnCode == 1) {
                t.setStatus(SUCCESS);
                t.setPaidAt(LocalDateTime.now());
                if (zpTransId != null) t.setProviderTransactionId(String.valueOf(zpTransId));
                repo.save(t);
                log.info("ZALOPAY {}: THÀNH CÔNG appTransId={} zpTransId={}", source, appTransId, zpTransId);
                return Boolean.TRUE;
            }
            if (returnCode == 2 && PENDING.equals(t.getStatus())) {
                t.setStatus(FAILED);
                repo.save(t);
                log.info("ZALOPAY {}: THẤT BẠI appTransId={} returnCode={}", source, appTransId, returnCode);
            }
            return Boolean.FALSE;
        });
        if (Boolean.TRUE.equals(newlySucceeded)) {
            try { finalizeOrder(appTransId); }
            catch (RuntimeException e) { log.error("ZALOPAY đã nhận tiền nhưng chưa hoàn tất đơn: appTransId={} lỗi={}", appTransId, e.getMessage()); }
        }
    }

    /** Tạo hóa đơn + chuyển đơn sang PAID (tái dùng InvoiceService.checkout: trừ kho, tích điểm, giải phóng bàn). */
    private void finalizeOrder(String appTransId) {
        tx.executeWithoutResult(status -> {
            PaymentTransaction t = repo.findWithLockByIdempotencyKey(appTransId).orElse(null);
            if (t == null || !SUCCESS.equals(t.getStatus()) || t.getInvoiceId() != null || t.getOrderId() == null) return;
            Order order = orderService.getById(t.getOrderId().intValue());
            if ("PAID".equalsIgnoreCase(order.getStatus())) {
                log.warn("ZALOPAY ĐỐI SOÁT: đơn #{} đã PAID từ trước, giao dịch {} cần kiểm tra/hoàn tiền", order.getId(), appTransId);
                return;
            }
            if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
                log.warn("ZALOPAY ĐỐI SOÁT: đơn #{} đã hủy nhưng khách đã trả tiền (giao dịch {}), cần hoàn tiền", order.getId(), appTransId);
                return;
            }
            if (serverTotal(order.getId()).compareTo(t.getAmount()) != 0) {
                log.warn("ZALOPAY ĐỐI SOÁT: tổng đơn #{} đã thay đổi sau khi tạo giao dịch {}, cần xử lý thủ công", order.getId(), appTransId);
                return;
            }
            Invoice invoice = invoiceService.checkout(new CheckoutRequest(order.getId(), null, "ZALOPAY"));
            t.setInvoiceId(invoice.getId().longValue());
            repo.save(t);
        });
    }

    private void markFailed(String appTransId) {
        tx.executeWithoutResult(status -> repo.findWithLockByIdempotencyKey(appTransId).ifPresent(t -> {
            if (PENDING.equals(t.getStatus())) { t.setStatus(FAILED); repo.save(t); }
        }));
    }

    private void expire(String appTransId) {
        tx.executeWithoutResult(status -> repo.findWithLockByIdempotencyKey(appTransId).ifPresent(t -> {
            if (PENDING.equals(t.getStatus())) { t.setStatus(EXPIRED); repo.save(t); log.info("ZALOPAY hết hạn: {}", appTransId); }
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
            throw new BadRequestException("ZaloPay chỉ hỗ trợ số tiền từ 1.000 đến 50.000.000 ₫ cho mỗi giao dịch");
        }
        return amount;
    }

    private ZalopayStatusResponse toStatus(PaymentTransaction t) {
        return new ZalopayStatusResponse(t.getIdempotencyKey(), t.getStatus(),
                t.getOrderId() == null ? null : t.getOrderId().intValue(), t.getAmount(), t.getProviderTransactionId());
    }

    private void requireConfigured() {
        if (!props.isConfigured()) throw new PaymentGatewayException("Thanh toán ZaloPay chưa được cấu hình trên máy chủ");
    }

    private static String requireText(Object v, String name) {
        if (v == null || String.valueOf(v).isBlank()) throw new BadRequestException("Thiếu trường " + name);
        return String.valueOf(v);
    }

    private static long requireNumber(Object v, String name) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        throw new BadRequestException("Thiếu hoặc sai trường " + name);
    }
}
