package com.restaurant.backend.payment;

import com.restaurant.backend.dto.CheckoutRequest;
import com.restaurant.backend.dto.MomoCreateResponse;
import com.restaurant.backend.dto.MomoStatusResponse;
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
import java.util.Set;

/**
 * Nghiệp vụ thanh toán MoMo.
 * Nguyên tắc: số tiền do server tính từ DB; chỉ tin chữ ký IPN hoặc kết quả truy vấn trực tiếp từ MoMo
 * (không tin tham số URL redirect); xử lý thành công đúng một lần nhờ khóa dòng giao dịch.
 */
@Service
public class MomoPaymentService {
    private static final Logger log = LoggerFactory.getLogger(MomoPaymentService.class);

    public static final String PROVIDER = "MOMO";
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String EXPIRED = "EXPIRED";

    // Giới hạn số tiền của MoMo (sandbox): 1.000 - 50.000.000 VND.
    private static final long MIN_AMOUNT = 1_000L;
    private static final long MAX_AMOUNT = 50_000_000L;
    // resultCode MoMo nghĩa là "chưa xong": khởi tạo, đang xử lý, chờ xác nhận, đã ủy quyền.
    private static final Set<Integer> PENDING_CODES = Set.of(1000, 7000, 7002, 9000);

    private final MomoProperties props;
    private final MomoClient client;
    private final PaymentTransactionRepository repo;
    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final TransactionTemplate tx;

    public MomoPaymentService(MomoProperties props, MomoClient client, PaymentTransactionRepository repo,
                              OrderService orderService, InvoiceService invoiceService, PlatformTransactionManager txManager) {
        this.props = props;
        this.client = client;
        this.repo = repo;
        this.orderService = orderService;
        this.invoiceService = invoiceService;
        this.tx = new TransactionTemplate(txManager);
    }

    public boolean isEnabled() { return props.isConfigured(); }

    public java.util.List<String> missingConfig() { return props.missing(); }

    // ------------------------------------------------------------------ tạo giao dịch

    public MomoCreateResponse createPayment(Integer orderId, String phone) {
        requireConfigured();
        Order order = loadOwnedOrder(orderId, phone);
        if (!"MOMO".equalsIgnoreCase(order.getPaymentPreference())) {
            throw new BadRequestException("Đơn hàng này không chọn thanh toán bằng MoMo");
        }
        // Nếu lần thanh toán trước thực ra đã thành công thì cập nhật trước, tránh thu tiền hai lần.
        for (PaymentTransaction old : repo.findByOrderIdAndProviderAndStatus(orderId.longValue(), PROVIDER, PENDING)) {
            try { refresh(old.getIdempotencyKey()); } catch (RuntimeException e) { log.warn("Không làm mới giao dịch cũ {}: {}", old.getIdempotencyKey(), e.getMessage()); }
        }
        order = loadOwnedOrder(orderId, phone);
        assertPayable(order);
        if (!repo.findByOrderIdAndProviderAndStatus(orderId.longValue(), PROVIDER, SUCCESS).isEmpty()) {
            throw new BadRequestException("Đơn hàng này đã được thanh toán bằng MoMo");
        }

        BigDecimal total = serverTotal(orderId);
        long amount = toWholeVnd(total);
        String momoOrderId = "HS" + orderId + "T" + System.currentTimeMillis();

        PaymentTransaction created = tx.execute(status -> {
            PaymentTransaction t = new PaymentTransaction();
            t.setOrderId(orderId.longValue());
            t.setProvider(PROVIDER);
            t.setIdempotencyKey(momoOrderId);
            t.setAmount(BigDecimal.valueOf(amount));
            t.setStatus(PENDING);
            return repo.save(t);
        });
        log.info("MOMO create: momoOrderId={} orderId={} amount={}", momoOrderId, orderId, amount);

        MomoGatewayResult result;
        try {
            result = client.create(momoOrderId, amount, "Thanh toan don hang #" + orderId + " - Han Seoul");
        } catch (RuntimeException e) {
            markFailed(momoOrderId);
            throw e;
        }
        if (result.resultCode() != 0 || result.payUrl() == null || result.payUrl().isBlank()) {
            markFailed(momoOrderId);
            log.warn("MOMO create bị từ chối: momoOrderId={} resultCode={}", momoOrderId, result.resultCode());
            throw new BadRequestException("MoMo từ chối tạo giao dịch" + (result.message() == null ? "" : ": " + result.message()));
        }
        return new MomoCreateResponse(created.getIdempotencyKey(), result.payUrl(), BigDecimal.valueOf(amount));
    }

    // ------------------------------------------------------------------ IPN

    /** Xử lý IPN từ MoMo. Ném BadRequestException nếu chữ ký/số tiền sai; trả bình thường nếu hợp lệ hoặc là bản lặp. */
    public void handleIpn(Map<String, Object> body) {
        requireConfigured();
        Map<String, Object> toVerify = new java.util.HashMap<>(body);
        toVerify.put("accessKey", props.getAccessKey());
        String expected = MomoSignature.forIpn(props.getSecretKey(), toVerify);
        String actual = body.get("signature") == null ? null : String.valueOf(body.get("signature"));
        if (!MomoSignature.matches(expected, actual)) {
            log.warn("MOMO IPN sai chữ ký: orderId={}", body.get("orderId"));
            throw new BadRequestException("Chữ ký không hợp lệ");
        }
        if (!props.getPartnerCode().equals(String.valueOf(body.get("partnerCode")))) {
            log.warn("MOMO IPN sai partnerCode: orderId={}", body.get("orderId"));
            throw new BadRequestException("partnerCode không khớp");
        }
        String momoOrderId = requireText(body.get("orderId"), "orderId");
        int resultCode = (int) requireNumber(body.get("resultCode"), "resultCode");
        long amount = requireNumber(body.get("amount"), "amount");
        Long transId = body.get("transId") instanceof Number n ? n.longValue() : null;
        log.info("MOMO IPN: momoOrderId={} resultCode={} transId={} amount={}", momoOrderId, resultCode, transId, amount);
        apply(momoOrderId, resultCode, amount, transId, "IPN");
    }

    // ------------------------------------------------------------------ truy vấn trạng thái

    /** Hỏi trực tiếp MoMo rồi cập nhật; dùng cho trang kết quả và job đối soát (IPN có thể không tới được máy local). */
    public MomoStatusResponse refresh(String momoOrderId) {
        requireConfigured();
        PaymentTransaction t = repo.findByIdempotencyKey(momoOrderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch"));
        if (PENDING.equals(t.getStatus()) || EXPIRED.equals(t.getStatus())) {
            MomoGatewayResult r = client.query(momoOrderId);
            apply(momoOrderId, r.resultCode(), r.amount(), r.transId(), "QUERY");
            t = repo.findByIdempotencyKey(momoOrderId).orElse(t);
        }
        return toStatus(t);
    }

    // ------------------------------------------------------------------ đối soát định kỳ

    /** Gọi từ MomoReconcileJob: làm mới giao dịch treo, đánh dấu hết hạn, hoàn tất đơn bị dang dở. */
    public void reconcile() {
        if (!isEnabled()) return;
        LocalDateTime now = LocalDateTime.now();
        List<PaymentTransaction> pending = repo.findByProviderAndStatusAndCreatedAtBetween(PROVIDER, PENDING, now.minusDays(2), now.minusMinutes(3));
        for (PaymentTransaction t : pending) {
            try {
                MomoStatusResponse s = refresh(t.getIdempotencyKey());
                if (PENDING.equals(s.status()) && t.getCreatedAt().isBefore(now.minusMinutes(30))) {
                    expire(t.getIdempotencyKey());
                }
            } catch (RuntimeException e) {
                log.warn("MOMO reconcile lỗi {}: {}", t.getIdempotencyKey(), e.getMessage());
            }
        }
        for (PaymentTransaction t : repo.findByProviderAndStatusAndInvoiceIdIsNullAndCreatedAtAfter(PROVIDER, SUCCESS, now.minusDays(7))) {
            try { finalizeOrder(t.getIdempotencyKey()); } catch (RuntimeException e) { log.error("MOMO không hoàn tất đơn cho giao dịch {}: {}", t.getIdempotencyKey(), e.getMessage()); }
        }
    }

    // ------------------------------------------------------------------ nội bộ

    /** Áp kết quả từ MoMo vào giao dịch: idempotent, kiểm tra số tiền, không bao giờ hạ SUCCESS về trạng thái khác. */
    private void apply(String momoOrderId, int resultCode, Long amount, Long transId, String source) {
        Boolean newlySucceeded = tx.execute(status -> {
            PaymentTransaction t = repo.findWithLockByIdempotencyKey(momoOrderId).orElse(null);
            if (t == null) {
                log.warn("MOMO {}: không có giao dịch {}", source, momoOrderId);
                return Boolean.FALSE;
            }
            if (SUCCESS.equals(t.getStatus())) {
                log.info("MOMO {}: bỏ qua bản lặp của giao dịch {}", source, momoOrderId);
                return Boolean.FALSE;
            }
            if (amount != null && t.getAmount().compareTo(BigDecimal.valueOf(amount)) != 0) {
                log.error("MOMO {}: SỐ TIỀN LỆCH momoOrderId={} expected={} actual={}", source, momoOrderId, t.getAmount(), amount);
                throw new BadRequestException("Số tiền không khớp");
            }
            if (resultCode == 0) {
                t.setStatus(SUCCESS);
                t.setPaidAt(LocalDateTime.now());
                if (transId != null) t.setProviderTransactionId(String.valueOf(transId));
                repo.save(t);
                log.info("MOMO {}: THÀNH CÔNG momoOrderId={} transId={}", source, momoOrderId, transId);
                return Boolean.TRUE;
            }
            if (!PENDING_CODES.contains(resultCode) && PENDING.equals(t.getStatus())) {
                t.setStatus(FAILED);
                repo.save(t);
                log.info("MOMO {}: THẤT BẠI momoOrderId={} resultCode={}", source, momoOrderId, resultCode);
            }
            return Boolean.FALSE;
        });
        if (Boolean.TRUE.equals(newlySucceeded)) {
            // Ghi nhận tiền về trước (đã commit), rồi mới ghi hóa đơn; nếu bước sau lỗi, job đối soát sẽ thử lại.
            try { finalizeOrder(momoOrderId); }
            catch (RuntimeException e) { log.error("MOMO đã nhận tiền nhưng chưa hoàn tất đơn: momoOrderId={} lỗi={}", momoOrderId, e.getMessage()); }
        }
    }

    /** Tạo hóa đơn + chuyển đơn sang PAID (tái dùng InvoiceService.checkout: trừ kho, tích điểm, giải phóng bàn). */
    private void finalizeOrder(String momoOrderId) {
        tx.executeWithoutResult(status -> {
            PaymentTransaction t = repo.findWithLockByIdempotencyKey(momoOrderId).orElse(null);
            if (t == null || !SUCCESS.equals(t.getStatus()) || t.getInvoiceId() != null || t.getOrderId() == null) return;
            Order order = orderService.getById(t.getOrderId().intValue());
            if ("PAID".equalsIgnoreCase(order.getStatus())) {
                log.warn("MOMO ĐỐI SOÁT: đơn #{} đã PAID từ trước, giao dịch {} cần kiểm tra/hoàn tiền", order.getId(), momoOrderId);
                return;
            }
            if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
                log.warn("MOMO ĐỐI SOÁT: đơn #{} đã hủy nhưng khách đã trả tiền (giao dịch {}), cần hoàn tiền", order.getId(), momoOrderId);
                return;
            }
            if (serverTotal(order.getId()).compareTo(t.getAmount()) != 0) {
                log.warn("MOMO ĐỐI SOÁT: tổng đơn #{} đã thay đổi sau khi tạo giao dịch {}, cần xử lý thủ công", order.getId(), momoOrderId);
                return;
            }
            Invoice invoice = invoiceService.checkout(new CheckoutRequest(order.getId(), null, "MOMO"));
            t.setInvoiceId(invoice.getId().longValue());
            repo.save(t);
        });
    }

    private void markFailed(String momoOrderId) {
        tx.executeWithoutResult(status -> repo.findWithLockByIdempotencyKey(momoOrderId).ifPresent(t -> {
            if (PENDING.equals(t.getStatus())) { t.setStatus(FAILED); repo.save(t); }
        }));
    }

    private void expire(String momoOrderId) {
        tx.executeWithoutResult(status -> repo.findWithLockByIdempotencyKey(momoOrderId).ifPresent(t -> {
            if (PENDING.equals(t.getStatus())) { t.setStatus(EXPIRED); repo.save(t); log.info("MOMO hết hạn: {}", momoOrderId); }
        }));
    }

    private Order loadOwnedOrder(Integer orderId, String phone) {
        String notFound = "Không tìm thấy đơn hàng với mã và số điện thoại đã cung cấp";
        Order order;
        try { order = orderService.getById(orderId); } catch (NotFoundException e) { throw new BadRequestException(notFound); }
        if (phone == null || order.getCustomerPhone() == null || !order.getCustomerPhone().equals(phone.trim())) {
            throw new BadRequestException(notFound); // không tiết lộ đơn có tồn tại hay không
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
            throw new BadRequestException("MoMo chỉ hỗ trợ số tiền từ 1.000 đến 50.000.000 ₫ cho mỗi giao dịch");
        }
        return amount;
    }

    private MomoStatusResponse toStatus(PaymentTransaction t) {
        return new MomoStatusResponse(t.getIdempotencyKey(), t.getStatus(), t.getOrderId() == null ? null : t.getOrderId().intValue(), t.getAmount());
    }

    private void requireConfigured() {
        if (!props.isConfigured()) throw new PaymentGatewayException("Thanh toán MoMo chưa được cấu hình trên máy chủ");
    }

    private static String requireText(Object v, String name) {
        if (v == null || String.valueOf(v).isBlank()) throw new BadRequestException("Thiếu trường " + name);
        return String.valueOf(v);
    }

    private static long requireNumber(Object v, String name) {
        if (v instanceof Number n) return n.longValue();
        throw new BadRequestException("Thiếu hoặc sai trường " + name);
    }
}
