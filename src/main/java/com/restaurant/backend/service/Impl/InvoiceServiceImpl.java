package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.CheckoutRequest;
import com.restaurant.backend.dto.RevenueReportResponse;
import com.restaurant.backend.dto.RevenueReportResponse.DailyRevenueItem;
import com.restaurant.backend.dto.RevenueReportResponse.PaymentMethodRevenueItem;
import com.restaurant.backend.entity.*;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.CustomerRepository;
import com.restaurant.backend.repository.IngredientRepository;
import com.restaurant.backend.repository.InvoiceRepository;
import com.restaurant.backend.repository.OrderItemRepository;
import com.restaurant.backend.repository.OrderRepository;
import com.restaurant.backend.repository.RecipeRepository;
import com.restaurant.backend.service.EmailService;
import com.restaurant.backend.service.InvoiceService;
import com.restaurant.backend.service.TableStateService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class InvoiceServiceImpl implements InvoiceService {
    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "CARD", "TRANSFER", "BANKING", "MOMO", "VNPAY", "ZALOPAY");

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TableStateService tableStateService;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final CustomerRepository customerRepository;
    private final EmailService emailService;
    private final EntityManager entityManager;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository,
                              OrderRepository orderRepository,
                              OrderItemRepository orderItemRepository,
                              TableStateService tableStateService,
                              RecipeRepository recipeRepository,
                              IngredientRepository ingredientRepository,
                              CustomerRepository customerRepository,
                              EmailService emailService,
                              EntityManager entityManager) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tableStateService = tableStateService;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.customerRepository = customerRepository;
        this.emailService = emailService;
        this.entityManager = entityManager;
    }

    @Override
    public Invoice checkout(CheckoutRequest request) {
        if (request == null || request.orderId() == null) {
            throw new BadRequestException("Vui lòng cung cấp mã đơn hàng cần thanh toán");
        }

        // Serialize checkout and item changes for this order, including gateway callbacks.
        Order order = orderRepository.findByIdForUpdate(request.orderId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng có mã ID: " + request.orderId()));
        entityManager.refresh(order, LockModeType.PESSIMISTIC_WRITE);
        if ("PAID".equalsIgnoreCase(order.getStatus()) || invoiceRepository.existsByOrderId(order.getId())) {
            throw new BadRequestException("Đơn hàng này đã có hóa đơn, không thể thanh toán lại");
        }
        if (!"OPEN".equalsIgnoreCase(order.getStatus()) && !"SERVING".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Chỉ thanh toán được đơn đang mở hoặc đang phục vụ");
        }

        List<OrderItem> items = orderItemRepository.findByOrderIdForUpdate(order.getId());
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Đơn hàng này chưa có món ăn nào, không thể thực hiện thanh toán");
        }
        items.forEach(item -> entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE));

        BigDecimal subtotal = BigDecimal.ZERO;
        // Aggregate shared ingredients across every dish before checking or changing stock.
        Map<Long, BigDecimal> requirements = new TreeMap<>();
        for (OrderItem item : items) {
            if (item.getId() == null || item.getId().getMenuItemId() == null
                    || item.getQuantity() == null || item.getQuantity() <= 0
                    || item.getUnitPrice() == null || item.getUnitPrice().signum() < 0) {
                throw new BadRequestException("Đơn hàng có món hoặc số lượng/đơn giá không hợp lệ");
            }
            subtotal = subtotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            List<Recipe> recipe = recipeRepository.findByMenuItem(item.getId().getMenuItemId().longValue());
            for (Recipe line : recipe) {
                if (line.getIngredient() == null || line.getIngredient().getId() == null
                        || line.getQuantityNeeded() == null || line.getQuantityNeeded().signum() <= 0) {
                    throw new BadRequestException("Công thức món ăn có nguyên liệu hoặc định lượng không hợp lệ");
                }
                BigDecimal needed = line.getQuantityNeeded().multiply(BigDecimal.valueOf(item.getQuantity()));
                requirements.merge(line.getIngredient().getId(), needed, BigDecimal::add);
            }
        }

        BigDecimal discount = request.discount() == null ? BigDecimal.ZERO : request.discount();
        if (discount.signum() < 0 || discount.compareTo(subtotal) > 0) {
            throw new BadRequestException("Tiền giảm giá phải từ 0 đến tổng tiền món ăn");
        }
        String method = request.paymentMethod() == null || request.paymentMethod().isBlank()
                ? "CASH" : request.paymentMethod().trim().toUpperCase(Locale.ROOT);
        if (!PAYMENT_METHODS.contains(method)) {
            throw new BadRequestException("Phương thức thanh toán không hợp lệ");
        }

        Map<Long, Ingredient> lockedIngredients = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : requirements.entrySet()) {
            Ingredient ingredient = ingredientRepository.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy nguyên liệu trong công thức: " + entry.getKey()));
            // Recipe loading may already have cached this entity before waiting for its lock.
            entityManager.refresh(ingredient, LockModeType.PESSIMISTIC_WRITE);
            BigDecimal available = ingredient.getStockQty() == null ? BigDecimal.ZERO : ingredient.getStockQty();
            if (available.compareTo(entry.getValue()) < 0) {
                throw new BadRequestException("Không đủ nguyên liệu " + ingredient.getName()
                        + ": cần " + entry.getValue().stripTrailingZeros().toPlainString()
                        + ", còn " + available.stripTrailingZeros().toPlainString() + " " + ingredient.getUnit());
            }
            lockedIngredients.put(entry.getKey(), ingredient);
        }

        Customer customer = null;
        BigDecimal total = subtotal.subtract(discount);
        int earnedPoints = total.divide(BigDecimal.valueOf(10000), 0, RoundingMode.FLOOR).intValueExact();
        if (order.getCustomerId() != null && earnedPoints > 0) {
            customer = customerRepository.findByIdForUpdate(order.getCustomerId().longValue())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy khách hàng của đơn hàng"));
            entityManager.refresh(customer, LockModeType.PESSIMISTIC_WRITE);
        }

        // All validation completes before any invoice, stock, order or points changes.
        Invoice invoice = new Invoice();
        invoice.setOrderId(order.getId());
        invoice.setSubtotal(subtotal);
        invoice.setDiscount(discount);
        invoice.setTotal(total);
        invoice.setPaymentMethod(method);
        invoice.setPaidAt(LocalDateTime.now());
        Invoice saved = invoiceRepository.save(invoice);

        for (Map.Entry<Long, Ingredient> entry : lockedIngredients.entrySet()) {
            Ingredient ingredient = entry.getValue();
            ingredient.setStockQty(ingredient.getStockQty().subtract(requirements.get(entry.getKey())));
            ingredientRepository.save(ingredient);
        }
        if (customer != null) {
            int currentPoints = customer.getLoyaltyPoints() == null ? 0 : customer.getLoyaltyPoints();
            customer.setLoyaltyPoints(Math.addExact(currentPoints, earnedPoints));
            customerRepository.save(customer);
        }
        order.setStatus("PAID");
        orderRepository.save(order);
        tableStateService.refresh(order.getTableId());

        Runnable sendEmail = () -> {
            try {
                emailService.sendPaymentSuccessEmail(order, saved, items);
            } catch (Exception exception) {
                org.slf4j.LoggerFactory.getLogger(InvoiceServiceImpl.class)
                        .warn("Lỗi kích hoạt gửi email thanh toán: {}", exception.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { sendEmail.run(); }
            });
        } else {
            sendEmail.run();
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(Integer days) {
        int dayCount = days == null ? 30 : days;
        if (dayCount < 1 || dayCount > 366) {
            throw new BadRequestException("Số ngày báo cáo phải từ 1 đến 366");
        }
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(dayCount - 1).atStartOfDay();
        LocalDateTime until = today.plusDays(1).atStartOfDay();
        List<Invoice> invoices = invoiceRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(from, until);
        Map<String, BigDecimal> revenueByDay = new TreeMap<>();
        Map<String, Long> countByDay = new HashMap<>();
        Map<String, BigDecimal> revenueByMethod = new TreeMap<>();
        Map<String, Long> countByMethod = new HashMap<>();
        for (int offset = dayCount - 1; offset >= 0; offset--) {
            String date = today.minusDays(offset).toString();
            revenueByDay.put(date, BigDecimal.ZERO);
            countByDay.put(date, 0L);
        }
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal todayRevenue = BigDecimal.ZERO;
        long count = 0;
        for (Invoice invoice : invoices) {
            LocalDateTime paidAt = invoice.getPaidAt();
            if (paidAt == null || paidAt.isBefore(from) || !paidAt.isBefore(until)) continue;
            BigDecimal amount = invoice.getTotal() == null ? BigDecimal.ZERO : invoice.getTotal();
            total = total.add(amount);
            count++;
            String date = paidAt.toLocalDate().toString();
            revenueByDay.merge(date, amount, BigDecimal::add);
            countByDay.merge(date, 1L, Long::sum);
            if (paidAt.toLocalDate().equals(today)) todayRevenue = todayRevenue.add(amount);
            String method = invoice.getPaymentMethod() == null || invoice.getPaymentMethod().isBlank()
                    ? "CASH" : invoice.getPaymentMethod().trim().toUpperCase(Locale.ROOT);
            revenueByMethod.merge(method, amount, BigDecimal::add);
            countByMethod.merge(method, 1L, Long::sum);
        }
        List<DailyRevenueItem> daily = revenueByDay.entrySet().stream()
                .map(entry -> new DailyRevenueItem(entry.getKey(), entry.getValue(), countByDay.get(entry.getKey())))
                .toList();
        List<PaymentMethodRevenueItem> methods = revenueByMethod.entrySet().stream()
                .map(entry -> new PaymentMethodRevenueItem(entry.getKey(), entry.getValue(), countByMethod.get(entry.getKey())))
                .toList();
        BigDecimal average = count == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP);
        return new RevenueReportResponse(total, count, average, todayRevenue, daily, methods);
    }

    @Override
    public Invoice create(Invoice invoice) {
        if (invoice == null) throw new BadRequestException("Thông tin hóa đơn không hợp lệ");
        // Legacy API must use the same validated checkout and server-calculated totals.
        return checkout(new CheckoutRequest(invoice.getOrderId(), invoice.getDiscount(), invoice.getPaymentMethod()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getAll() { return invoiceRepository.findAll(); }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getByOrderId(Integer orderId) { return invoiceRepository.findByOrderId(orderId); }

    @Override
    @Transactional(readOnly = true)
    public Invoice getById(Integer id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn có mã ID: " + id));
    }

    @Override
    public Invoice update(Integer id, Invoice invoice) {
        getById(id);
        throw new BadRequestException("Hóa đơn đã phát hành không được sửa trực tiếp");
    }

    @Override
    public void delete(Integer id) {
        getById(id);
        throw new BadRequestException("Hóa đơn đã phát hành không được xóa trực tiếp");
    }
}
