package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.OrderTotals;
import com.restaurant.backend.dto.CreateOrderRequest;
import com.restaurant.backend.dto.OrderItemRequest;
import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.entity.TableStatus;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.DiningTableRepository;
import com.restaurant.backend.repository.OrderItemRepository;
import com.restaurant.backend.repository.OrderRepository;
import com.restaurant.backend.repository.InvoiceRepository;
import com.restaurant.backend.repository.CustomerRepository;
import com.restaurant.backend.repository.EmployeeRepository;
import com.restaurant.backend.mapper.OrderItemMapper;
import com.restaurant.backend.service.OrderItemService;
import com.restaurant.backend.service.TableStateService;
import com.restaurant.backend.service.OrderService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * =========================================================================
 * LỚP THỰC THI NGHIỆP VỤ ĐƠN HÀNG (OrderServiceImpl)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Đơn hàng)
 * 
 * Mục đích:
 * Thực hiện chi tiết toàn bộ nghiệp vụ liên quan đến đơn hàng:
 * 1. Mở đơn hàng mới (tự động đổi trạng thái bàn ăn sang OCCUPIED - Đang dùng).
 * 2. Lấy danh sách, xem chi tiết, sửa thông tin đơn hàng.
 * 3. Thay đổi trạng thái đơn hàng (OPEN -> SERVING -> PAID...).
 * 4. Xóa đơn hàng và các món ăn liên quan.
 * 5. Tính toán tổng số tiền và số lượng món ăn của đơn.
 * =========================================================================
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class OrderServiceImpl implements OrderService {

    // Danh sách các trạng thái hợp lệ của một đơn hàng
    private static final Set<String> CAC_TRANG_THAI_HOP_LE = Set.of("OPEN", "SERVING", "PAID", "CANCELLED");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DiningTableRepository diningTableRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final OrderItemService orderItemService;
    private final OrderItemMapper orderItemMapper;
    private final TableStateService tableStateService;
    private final EntityManager entityManager;

    // Tiêm các Repository vào Service qua Constructor
    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            DiningTableRepository diningTableRepository,
            InvoiceRepository invoiceRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            OrderItemService orderItemService,
            OrderItemMapper orderItemMapper,
            TableStateService tableStateService,
            EntityManager entityManager) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.diningTableRepository = diningTableRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.orderItemService = orderItemService;
        this.orderItemMapper = orderItemMapper;
        this.tableStateService = tableStateService;
        this.entityManager = entityManager;
    }

    /**
     * BƯỚC 1: TẠO ĐƠN HÀNG MỚI (Mở bàn cho khách)
     */
    @Override
    public Order create(Order order) {
        if (order == null) throw new BadRequestException("Thiếu thông tin đơn hàng");
        if (order.getStatus() != null && !"OPEN".equalsIgnoreCase(order.getStatus())) {
            throw new BadRequestException("Đơn mới phải ở trạng thái Mới tạo");
        }
        validateReferences(order);
        // Kiểm tra dữ liệu dành cho đơn do khách tự đặt trên website.
        if (order.getOrderType() != null) {
            String type = order.getOrderType().trim().toUpperCase(Locale.ROOT);
            if (!Set.of("DINE_IN", "TAKEAWAY", "DELIVERY").contains(type)) {
                throw new BadRequestException("Hình thức nhận món không hợp lệ");
            }
            order.setOrderType(type);
            if (order.getCustomerName() == null || order.getCustomerName().isBlank()) {
                throw new BadRequestException("Vui lòng cung cấp họ tên người nhận");
            }
            if (order.getCustomerPhone() == null || !order.getCustomerPhone().matches("^0[0-9]{9,10}$")) {
                throw new BadRequestException("Số điện thoại người nhận không hợp lệ");
            }
            if ("DELIVERY".equals(type) && (order.getDeliveryAddress() == null || order.getDeliveryAddress().trim().length() < 8)) {
                throw new BadRequestException("Vui lòng cung cấp địa chỉ giao hàng đầy đủ");
            }
            if (order.getPaymentPreference() != null && !Set.of("CASH", "BANKING", "MOMO", "VNPAY", "ZALOPAY").contains(order.getPaymentPreference().toUpperCase(Locale.ROOT))) {
                throw new BadRequestException("Phương thức thanh toán không hợp lệ");
            }
        }
        // Đặt ID là null để CSDL tự sinh khóa chính tăng dần (Auto Increment)
        order.setId(null);

        // Nếu chưa có thời gian tạo, gán thời gian hiện tại
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }

        order.setStatus("OPEN");

        // Lưu đơn hàng vào bảng orders trong CSDL
        Order donHangMoi = orderRepository.save(order);

        tableStateService.refresh(donHangMoi.getTableId());

        return donHangMoi;
    }

    @Override
    public Order createWithItems(CreateOrderRequest request) {
        if (request == null || request.tableId() == null || request.items() == null
                || request.items().isEmpty() || request.items().size() > 50) {
            throw new BadRequestException("Vui lòng chọn bàn và từ 1 đến 50 món");
        }
        Set<Integer> menuIds = new HashSet<>();
        for (CreateOrderRequest.Line line : request.items()) {
            if (line == null || line.menuItemId() == null || line.quantity() == null
                    || line.quantity() < 1 || line.quantity() > 1000 || !menuIds.add(line.menuItemId())) {
                throw new BadRequestException("Món không hợp lệ, trùng món hoặc số lượng ngoài khoảng 1–1000");
            }
        }
        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setTableId(request.tableId());
        order.setEmployeeId(request.employeeId());
        Order saved = create(order);
        for (CreateOrderRequest.Line line : request.items()) {
            orderItemService.create(orderItemMapper.toEntity(new OrderItemRequest(
                    saved.getId(), line.menuItemId(), line.quantity(), null, line.note())));
        }
        return saved;
    }

    /**
     * BƯỚC 2: LẤY DANH SÁCH ĐƠN HÀNG
     */
    @Override
    @Transactional(readOnly = true)
    public List<Order> getAll(Integer customerId, String status) {
        List<Order> danhSachDonHang;

        // Nếu có lọc theo mã khách hàng
        if (customerId != null) {
            danhSachDonHang = orderRepository.findByCustomerIdOrderByIdDesc(customerId);
        } else {
            // Lấy tất cả đơn hàng
            danhSachDonHang = orderRepository.findAll();
        }

        // Nếu có lọc thêm theo trạng thái (ví dụ chỉ lấy đơn "OPEN")
        if (status != null && !status.trim().isEmpty()) {
            List<Order> ketQuaLoc = new ArrayList<>();
            for (Order don : danhSachDonHang) {
                if (status.equalsIgnoreCase(don.getStatus())) {
                    ketQuaLoc.add(don);
                }
            }
            return ketQuaLoc;
        }

        return danhSachDonHang;
    }

    /**
     * BƯỚC 3: LẤY THÔNG TIN 1 ĐƠN HÀNG THEO ID
     */
    @Override
    @Transactional(readOnly = true)
    public Order getById(Integer id) {
        // Tìm trong CSDL, nếu không tìm thấy thì ném ngoại lệ NotFoundException (mã lỗi 404)
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng có mã ID: " + id));
    }

    /**
     * BƯỚC 4: CẬP NHẬT THÔNG TIN ĐƠN HÀNG
     */
    @Override
    public Order update(Integer id, Order orderMoi) {
        // Lấy đơn hàng hiện tại trong DB ra kiểm tra
        Order donHangCu = lockOpenOrder(id);
        if (orderMoi.getStatus() != null && !orderMoi.getStatus().equalsIgnoreCase(donHangCu.getStatus())) {
            throw new BadRequestException("Hãy dùng thao tác đổi trạng thái hoặc thanh toán để kết thúc đơn");
        }
        Integer oldTableId = donHangCu.getTableId();
        // Lock both tables in a stable order so two simultaneous transfers cannot deadlock.
        java.util.stream.Stream.of(oldTableId, orderMoi.getTableId()).filter(Objects::nonNull)
                .distinct().sorted().forEach(this::lockTable);
        validateReferences(orderMoi);

        // Cập nhật lại các thông tin mới
        donHangCu.setCustomerId(orderMoi.getCustomerId());
        donHangCu.setTableId(orderMoi.getTableId());
        donHangCu.setEmployeeId(orderMoi.getEmployeeId());
        if (orderMoi.getCustomerName() != null) donHangCu.setCustomerName(orderMoi.getCustomerName());
        if (orderMoi.getCustomerPhone() != null) donHangCu.setCustomerPhone(orderMoi.getCustomerPhone());
        if (orderMoi.getCustomerEmail() != null) donHangCu.setCustomerEmail(orderMoi.getCustomerEmail());
        if (orderMoi.getOrderType() != null) donHangCu.setOrderType(orderMoi.getOrderType());
        if (orderMoi.getDeliveryAddress() != null) donHangCu.setDeliveryAddress(orderMoi.getDeliveryAddress());
        if (orderMoi.getCustomerNote() != null) donHangCu.setCustomerNote(orderMoi.getCustomerNote());
        if (orderMoi.getPaymentPreference() != null) donHangCu.setPaymentPreference(orderMoi.getPaymentPreference());

        // Lưu lại vào CSDL
        Order saved = orderRepository.save(donHangCu);
        tableStateService.refresh(oldTableId);
        if (!Objects.equals(oldTableId, saved.getTableId())) tableStateService.refresh(saved.getTableId());
        return saved;
    }

    /**
     * BƯỚC 5: ĐỔI TRẠNG THÁI ĐƠN HÀNG
     */
    @Override
    public Order updateStatus(Integer id, String statusMoi) {
        // Kiểm tra tính hợp lệ của trạng thái gửi lên
        if (statusMoi == null || !CAC_TRANG_THAI_HOP_LE.contains(statusMoi.toUpperCase())) {
            throw new BadRequestException("Trạng thái đơn hàng không hợp lệ. Các trạng thái gồm: " + CAC_TRANG_THAI_HOP_LE);
        }

        String next = statusMoi.toUpperCase(Locale.ROOT);
        Order donHang = lockOpenOrder(id);
        if (next.equals(donHang.getStatus())) return donHang;
        if (!("CANCELLED".equals(next) || ("OPEN".equals(donHang.getStatus()) && "SERVING".equals(next)))) {
            throw new BadRequestException("Chuyển trạng thái không hợp lệ. Đã thanh toán chỉ được ghi nhận qua Thanh toán");
        }
        donHang.setStatus(next);
        Order saved = orderRepository.save(donHang);
        tableStateService.refresh(saved.getTableId());
        return saved;
    }

    /**
     * BƯỚC 6: XÓA ĐƠN HÀNG
     */
    @Override
    public void delete(Integer id) {
        Order donHang = lockOpenOrder(id);

        // Xóa tất cả các món ăn thuộc về đơn hàng này trước
        orderItemRepository.deleteByIdOrderId(id);

        // Sau đó xóa bản ghi đơn hàng
        orderRepository.delete(donHang);
        tableStateService.refresh(donHang.getTableId());
    }

    private Order lockOpenOrder(Integer id) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng " + id));
        entityManager.refresh(order);
        if (!Set.of("OPEN", "SERVING").contains(order.getStatus())
                || invoiceRepository.findFirstByOrderId(id).isPresent()) {
            throw new BadRequestException("Đơn đã kết thúc, không thể chỉnh sửa");
        }
        return order;
    }

    private DiningTable lockTable(Integer id) {
        if (id <= 0) throw new BadRequestException("Mã bàn không hợp lệ");
        DiningTable table = diningTableRepository.findByIdForUpdate(id.longValue())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bàn " + id));
        entityManager.refresh(table);
        if (Boolean.TRUE.equals(table.getDeleted())) throw new BadRequestException("Bàn đã bị xóa");
        return table;
    }

    private void validateReferences(Order order) {
        if (order.getTableId() != null) lockTable(order.getTableId());
        if (order.getCustomerId() != null && (order.getCustomerId() <= 0
                || customerRepository.findById(order.getCustomerId().longValue())
                    .filter(c -> !Boolean.TRUE.equals(c.getDeleted())).isEmpty())) {
            throw new BadRequestException("Khách hàng không tồn tại hoặc đã bị xóa");
        }
        if (order.getEmployeeId() != null && (order.getEmployeeId() <= 0
                || employeeRepository.findById(order.getEmployeeId().longValue())
                    .filter(e -> !Boolean.TRUE.equals(e.getDeleted())).isEmpty())) {
            throw new BadRequestException("Nhân viên không tồn tại hoặc đã bị xóa");
        }
    }

    /**
     * BƯỚC 7: TÍNH TỔNG TIỀN VÀ SỐ MÓN CỦA CÁC ĐƠN HÀNG
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Integer, OrderTotals> totals(Collection<Integer> orderIds) {
        Map<Integer, OrderTotals> ketQua = new HashMap<>();

        if (orderIds == null || orderIds.isEmpty()) {
            return ketQua;
        }

        // Gọi câu lệnh gom nhóm tổng hợp từ Repository
        List<Object[]> danhSachTong = orderItemRepository.aggregateByOrderIds(orderIds);

        for (Object[] dong : danhSachTong) {
            Integer maDonHang = ((Number) dong[0]).intValue();
            BigDecimal tongTien = (dong[1] == null) ? BigDecimal.ZERO : new BigDecimal(dong[1].toString());
            long tongSoLuong = (dong[2] == null) ? 0L : ((Number) dong[2]).longValue();

            ketQua.put(maDonHang, new OrderTotals(tongTien, tongSoLuong));
        }

        return ketQua;
    }
}
