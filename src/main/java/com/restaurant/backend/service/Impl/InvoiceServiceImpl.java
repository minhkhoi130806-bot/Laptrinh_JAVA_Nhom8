package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.CheckoutRequest;
import com.restaurant.backend.entity.*;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.DiningTableRepository;
import com.restaurant.backend.repository.InvoiceRepository;
import com.restaurant.backend.repository.OrderItemRepository;
import com.restaurant.backend.repository.OrderRepository;
import com.restaurant.backend.service.InvoiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * =========================================================================
 * LỚP THỰC THI NGHIỆP VỤ HÓA ĐƠN & THANH TOÁN (InvoiceServiceImpl)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Thanh toán Hóa đơn)
 * 
 * Mục đích và yêu cầu cốt lõi của Tuần 3:
 * API thanh toán tạo INVOICE (@Transactional):
 * 1. Tính toán chính xác tổng tiền từ các món ăn đã gọi trong đơn (subtotal).
 * 2. Trừ chiết khấu / giảm giá nếu có (discount) để ra tổng thanh toán (total).
 * 3. Tạo bản ghi hóa đơn lưu trữ phương thức trả tiền (tiền mặt, thẻ, chuyển khoản).
 * 4. Cập nhật trạng thái của đơn hàng thành "PAID" (Đã thanh toán).
 * 5. Tự động chuyển trạng thái của bàn ăn về AVAILABLE (Bàn trống, sẵn sàng đón khách tiếp theo).
 * 6. Toàn bộ logic này được bảo vệ bởi @Transactional: Đảm bảo nếu một bước bị lỗi,
 *    toàn bộ các thay đổi trên CSDL sẽ được quay ngược lại (rollback), không làm hỏng dữ liệu.
 * =========================================================================
 */
@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DiningTableRepository diningTableRepository;

    // Tiêm các Repository vào Service qua Constructor
    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            DiningTableRepository diningTableRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.diningTableRepository = diningTableRepository;
    }

    /**
     * BƯỚC QUAN TRỌNG NHẤT TUẦN 3: THANH TOÁN ĐƠN HÀNG VÀ TẠO HÓA ĐƠN
     * Sử dụng @Transactional để gộp tất cả các thao tác thành một giao dịch duy nhất.
     */
    @Override
    @Transactional
    public Invoice checkout(CheckoutRequest request) {
        // Bước 1: Kiểm tra mã đơn hàng gửi lên có hợp lệ không
        if (request.orderId() == null) {
            throw new BadRequestException("Vui lòng cung cấp mã đơn hàng cần thanh toán");
        }

        Integer maDonHang = request.orderId();

        // Bước 2: Tìm đơn hàng trong cơ sở dữ liệu
        Order donHang = orderRepository.findById(maDonHang)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng có mã ID: " + maDonHang));

        // Bước 3: Kiểm tra xem đơn hàng đã từng thanh toán trước đó chưa
        if ("PAID".equalsIgnoreCase(donHang.getStatus())) {
            throw new BadRequestException("Đơn hàng này đã được thanh toán rồi, không thể thanh toán lại!");
        }

        // Bước 4: Lấy danh sách tất cả các món ăn mà khách đã gọi trong đơn hàng này
        List<OrderItem> danhSachMonAn = orderItemRepository.findByIdOrderId(maDonHang);

        if (danhSachMonAn == null || danhSachMonAn.isEmpty()) {
            throw new BadRequestException("Đơn hàng này chưa có món ăn nào, không thể thực hiện thanh toán!");
        }

        // Bước 5: Tính tổng số tiền các món ăn (subtotal)
        // subtotal = Tổng của (số lượng * đơn giá) của từng món
        BigDecimal tongTienMon = BigDecimal.ZERO;

        for (OrderItem mon : danhSachMonAn) {
            int soLuong = (mon.getQuantity() != null) ? mon.getQuantity() : 0;
            BigDecimal donGia = (mon.getUnitPrice() != null) ? mon.getUnitPrice() : BigDecimal.ZERO;

            // Tiền của món này = số lượng * đơn giá
            BigDecimal tienMonNay = donGia.multiply(BigDecimal.valueOf(soLuong));

            // Cộng dồn vào tổng tiền
            tongTienMon = tongTienMon.add(tienMonNay);
        }

        // Bước 6: Xử lý số tiền giảm giá (discount) nếu có
        BigDecimal giamGia = BigDecimal.ZERO;
        if (request.discount() != null && request.discount().compareTo(BigDecimal.ZERO) > 0) {
            giamGia = request.discount();
        }

        // Bước 7: Tính tổng tiền khách phải trả thực tế (total = subtotal - discount)
        BigDecimal tongThanhToan = tongTienMon.subtract(giamGia);

        // Đảm bảo số tiền thanh toán không bao giờ bị âm
        if (tongThanhToan.compareTo(BigDecimal.ZERO) < 0) {
            tongThanhToan = BigDecimal.ZERO;
        }

        // Bước 8: Tạo đối tượng Hóa đơn mới (Invoice) và gán các thông tin
        Invoice hoaDon = new Invoice();
        hoaDon.setId(null); // Để CSDL tự tăng ID
        hoaDon.setOrderId(maDonHang);
        hoaDon.setSubtotal(tongTienMon);
        hoaDon.setDiscount(giamGia);
        hoaDon.setTotal(tongThanhToan);

        // Phương thức thanh toán (mặc định là "CASH" - Tiền mặt nếu không truyền)
        String phuongThuc = (request.paymentMethod() != null && !request.paymentMethod().trim().isEmpty())
                ? request.paymentMethod().trim().toUpperCase()
                : "CASH";
        hoaDon.setPaymentMethod(phuongThuc);

        // Thời gian thanh toán là thời điểm hiện tại
        hoaDon.setPaidAt(LocalDateTime.now());

        // Lưu hóa đơn vào CSDL
        Invoice hoaDonDaLuu = invoiceRepository.save(hoaDon);

        // Bước 9: CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG THÀNH "PAID" (Đã thanh toán)
        donHang.setStatus("PAID");
        orderRepository.save(donHang);

        // Bước 10: TỰ ĐỘNG GIẢI PHÓNG BÀN ĂN (Nếu đơn hàng có gắn với 1 bàn)
        if (donHang.getTableId() != null) {
            Long maBan = Long.valueOf(donHang.getTableId());
            Optional<DiningTable> banAnOpt = diningTableRepository.findById(maBan);

            if (banAnOpt.isPresent()) {
                DiningTable banAn = banAnOpt.get();
                // Đổi trạng thái bàn thành AVAILABLE (Bàn trống, sẵn sàng nhận khách mới)
                banAn.setStatus(TableStatus.AVAILABLE);
                diningTableRepository.save(banAn);
            }
        }

        // Bước 11: Trả về hóa đơn vừa thanh toán thành công
        return hoaDonDaLuu;
    }

    /**
     * BƯỚC 12: TẠO HÓA ĐƠN THỦ CÔNG
     */
    @Override
    public Invoice create(Invoice invoice) {
        invoice.setId(null);
        if (invoice.getPaidAt() == null) {
            invoice.setPaidAt(LocalDateTime.now());
        }
        return invoiceRepository.save(invoice);
    }

    /**
     * BƯỚC 13: LẤY DANH SÁCH TOÀN BỘ HÓA ĐƠN
     */
    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getAll() {
        return invoiceRepository.findAll();
    }

    /**
     * BƯỚC 14: LẤY HÓA ĐƠN CỦA 1 ĐƠN HÀNG CỤ THỂ
     */
    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getByOrderId(Integer orderId) {
        return invoiceRepository.findByOrderId(orderId);
    }

    /**
     * BƯỚC 15: TÌM HÓA ĐƠN THEO ID
     */
    @Override
    @Transactional(readOnly = true)
    public Invoice getById(Integer id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn có mã ID: " + id));
    }

    /**
     * BƯỚC 16: CẬP NHẬT HÓA ĐƠN
     */
    @Override
    public Invoice update(Integer id, Invoice invoiceMoi) {
        Invoice hoaDonCu = getById(id);

        hoaDonCu.setOrderId(invoiceMoi.getOrderId());
        hoaDonCu.setSubtotal(invoiceMoi.getSubtotal());
        hoaDonCu.setDiscount(invoiceMoi.getDiscount());
        hoaDonCu.setTotal(invoiceMoi.getTotal());
        hoaDonCu.setPaymentMethod(invoiceMoi.getPaymentMethod());
        if (invoiceMoi.getPaidAt() != null) {
            hoaDonCu.setPaidAt(invoiceMoi.getPaidAt());
        }

        return invoiceRepository.save(hoaDonCu);
    }

    /**
     * BƯỚC 17: XÓA HÓA ĐƠN
     */
    @Override
    public void delete(Integer id) {
        Invoice hoaDon = getById(id);
        invoiceRepository.delete(hoaDon);
    }
}
