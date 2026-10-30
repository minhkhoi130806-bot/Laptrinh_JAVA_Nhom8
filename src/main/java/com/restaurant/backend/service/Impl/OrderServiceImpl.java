package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.OrderTotals;
import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.Order;
import com.restaurant.backend.entity.TableStatus;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.DiningTableRepository;
import com.restaurant.backend.repository.OrderItemRepository;
import com.restaurant.backend.repository.OrderRepository;
import com.restaurant.backend.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional
public class OrderServiceImpl implements OrderService {

    // Danh sách các trạng thái hợp lệ của một đơn hàng
    private static final Set<String> CAC_TRANG_THAI_HOP_LE = Set.of("OPEN", "SERVING", "PAID", "CANCELLED");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DiningTableRepository diningTableRepository;

    // Tiêm các Repository vào Service qua Constructor
    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            DiningTableRepository diningTableRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.diningTableRepository = diningTableRepository;
    }

    /**
     * BƯỚC 1: TẠO ĐƠN HÀNG MỚI (Mở bàn cho khách)
     */
    @Override
    public Order create(Order order) {
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
            if (order.getPaymentPreference() != null && !Set.of("CASH", "BANKING", "MOMO").contains(order.getPaymentPreference().toUpperCase(Locale.ROOT))) {
                throw new BadRequestException("Phương thức thanh toán không hợp lệ");
            }
        }
        // Đặt ID là null để CSDL tự sinh khóa chính tăng dần (Auto Increment)
        order.setId(null);

        // Nếu chưa có thời gian tạo, gán thời gian hiện tại
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }

        // Trạng thái mặc định ban đầu của đơn hàng là OPEN (Đang mở)
        if (order.getStatus() == null || order.getStatus().trim().isEmpty()) {
            order.setStatus("OPEN");
        }

        // Lưu đơn hàng vào bảng orders trong CSDL
        Order donHangMoi = orderRepository.save(order);

        // Nếu đơn hàng này gắn với một bàn ăn cụ thể (tableId khác null)
        if (donHangMoi.getTableId() != null) {
            Long maBan = Long.valueOf(donHangMoi.getTableId());
            Optional<DiningTable> banAnOpt = diningTableRepository.findById(maBan);

            // Tự động chuyển trạng thái bàn ăn sang OCCUPIED (Đang có khách ngồi)
            if (banAnOpt.isPresent()) {
                DiningTable banAn = banAnOpt.get();
                banAn.setStatus(TableStatus.OCCUPIED);
                diningTableRepository.save(banAn);
            }
        }

        return donHangMoi;
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
        Order donHangCu = getById(id);

        // Cập nhật lại các thông tin mới
        donHangCu.setCustomerId(orderMoi.getCustomerId());
        donHangCu.setTableId(orderMoi.getTableId());
        donHangCu.setEmployeeId(orderMoi.getEmployeeId());
        if (orderMoi.getCreatedAt() != null) {
            donHangCu.setCreatedAt(orderMoi.getCreatedAt());
        }
        if (orderMoi.getStatus() != null) {
            donHangCu.setStatus(orderMoi.getStatus());
        }
        if (orderMoi.getCustomerName() != null) donHangCu.setCustomerName(orderMoi.getCustomerName());
        if (orderMoi.getCustomerPhone() != null) donHangCu.setCustomerPhone(orderMoi.getCustomerPhone());
        if (orderMoi.getCustomerEmail() != null) donHangCu.setCustomerEmail(orderMoi.getCustomerEmail());
        if (orderMoi.getOrderType() != null) donHangCu.setOrderType(orderMoi.getOrderType());
        if (orderMoi.getDeliveryAddress() != null) donHangCu.setDeliveryAddress(orderMoi.getDeliveryAddress());
        if (orderMoi.getCustomerNote() != null) donHangCu.setCustomerNote(orderMoi.getCustomerNote());
        if (orderMoi.getPaymentPreference() != null) donHangCu.setPaymentPreference(orderMoi.getPaymentPreference());

        // Lưu lại vào CSDL
        return orderRepository.save(donHangCu);
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

        // Lấy đơn hàng và gán trạng thái mới
        Order donHang = getById(id);
        donHang.setStatus(statusMoi.toUpperCase());

        return orderRepository.save(donHang);
    }

    /**
     * BƯỚC 6: XÓA ĐƠN HÀNG
     */
    @Override
    public void delete(Integer id) {
        Order donHang = getById(id);

        // Xóa tất cả các món ăn thuộc về đơn hàng này trước
        orderItemRepository.deleteByIdOrderId(id);

        // Sau đó xóa bản ghi đơn hàng
        orderRepository.delete(donHang);
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
