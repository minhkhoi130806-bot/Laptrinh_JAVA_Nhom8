package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.CheckoutRequest;
import com.restaurant.backend.dto.RevenueReportResponse;
import com.restaurant.backend.dto.RevenueReportResponse.DailyRevenueItem;
import com.restaurant.backend.dto.RevenueReportResponse.PaymentMethodRevenueItem;
import com.restaurant.backend.entity.*;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.CustomerRepository;
import com.restaurant.backend.repository.DiningTableRepository;
import com.restaurant.backend.repository.IngredientRepository;
import com.restaurant.backend.repository.InvoiceRepository;
import com.restaurant.backend.repository.OrderItemRepository;
import com.restaurant.backend.repository.OrderRepository;
import com.restaurant.backend.repository.RecipeRepository;
import com.restaurant.backend.service.InvoiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * =========================================================================
 * LỚP THỰC THI NGHIỆP VỤ HÓA ĐƠN & THANH TOÁN (InvoiceServiceImpl)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 & Tuần 4 - Nghiệp vụ Thanh toán, Trừ kho & Báo cáo)
 * 
 * Mục đích và yêu cầu cốt lõi theo kế hoạch 5 tuần:
 * 1. API thanh toán tạo INVOICE (@Transactional):
 *    - Tính tổng tiền món ăn (subtotal), giảm giá (discount) và tổng thanh toán (total).
 *    - Tạo hóa đơn lưu phương thức thanh toán, thời gian thanh toán.
 *    - Cập nhật trạng thái đơn hàng thành "PAID".
 *    - Giải phóng bàn ăn (chuyển trạng thái bàn về AVAILABLE).
 *    - YÊU CẦU TUẦN 4 (Khôi): Tự động trừ kho nguyên liệu theo RECIPE trong cùng một giao dịch (@Transactional).
 *    - YÊU CẦU TUẦN 4 (Khôi - Phúc): Tự động cộng điểm tích lũy (loyalty_points) cho khách hàng.
 * 2. YÊU CẦU TUẦN 4 (Khôi): API báo cáo thống kê doanh thu theo ngày/tháng và theo phương thức thanh toán.
 * =========================================================================
 */
@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DiningTableRepository diningTableRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final CustomerRepository customerRepository;

    // Tiêm đầy đủ các Repository vào Service qua Constructor
    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            DiningTableRepository diningTableRepository,
            RecipeRepository recipeRepository,
            IngredientRepository ingredientRepository,
            CustomerRepository customerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.diningTableRepository = diningTableRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * BƯỚC QUAN TRỌNG: THANH TOÁN ĐƠN HÀNG VÀ TẠO HÓA ĐƠN (@Transactional)
     * Toàn bộ các thao tác: tạo Invoice, đổi status Order, đổi status Table,
     * trừ kho theo Recipe và tích điểm khách hàng đều diễn ra trong cùng 1 transaction.
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

        // =========================================================================
        // Bước 11 (KẾ HOẠCH TUẦN 4 - KHÔI): TRỪ KHO NGUYÊN LIỆU THEO RECIPE KHI THANH TOÁN
        // Cùng nằm trong transaction: Đảm bảo nguyên liệu được trừ chính xác vào kho
        // =========================================================================
        for (OrderItem mon : danhSachMonAn) {
            if (mon.getId() != null && mon.getId().getMenuItemId() != null) {
                Long maMon = Long.valueOf(mon.getId().getMenuItemId());
                int soLuongMon = (mon.getQuantity() != null) ? mon.getQuantity() : 0;

                if (soLuongMon > 0) {
                    // Lấy toàn bộ công thức cấu thành nên món ăn này
                    List<Recipe> congThucCuaMon = recipeRepository.findByMenuItem(maMon);

                    if (congThucCuaMon != null && !congThucCuaMon.isEmpty()) {
                        for (Recipe r : congThucCuaMon) {
                            Ingredient nguyenLieu = r.getIngredient();
                            if (nguyenLieu != null) {
                                // Lượng cần trừ = định lượng 1 món * số lượng món khách gọi
                                BigDecimal dinhLuong = (r.getQuantityNeeded() != null) ? r.getQuantityNeeded() : BigDecimal.ZERO;
                                BigDecimal luongCanTru = dinhLuong.multiply(BigDecimal.valueOf(soLuongMon));

                                // Tồn kho hiện tại
                                BigDecimal tonHienTai = (nguyenLieu.getStockQty() != null) ? nguyenLieu.getStockQty() : BigDecimal.ZERO;
                                BigDecimal tonMoi = tonHienTai.subtract(luongCanTru);

                                // Tránh để tồn kho bị âm
                                if (tonMoi.compareTo(BigDecimal.ZERO) < 0) {
                                    tonMoi = BigDecimal.ZERO;
                                }

                                nguyenLieu.setStockQty(tonMoi);
                                ingredientRepository.save(nguyenLieu);
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // Bước 12 (KẾ HOẠCH TUẦN 4 - KHÔI): TÍCH ĐIỂM THƯỞNG CHO KHÁCH HÀNG (Loyalty points)
        // Mỗi 10.000 VNĐ thanh toán được cộng 1 điểm tích lũy
        // =========================================================================
        if (donHang.getCustomerId() != null) {
            Long maKhachHang = Long.valueOf(donHang.getCustomerId());
            Optional<Customer> khachHangOpt = customerRepository.findById(maKhachHang);

            if (khachHangOpt.isPresent()) {
                Customer khachHang = khachHangOpt.get();
                // Tính điểm cộng: 1 điểm cho mỗi 10.000 VNĐ thanh toán thực tế
                int diemCong = tongThanhToan.divide(BigDecimal.valueOf(10000), 0, RoundingMode.FLOOR).intValue();

                if (diemCong > 0) {
                    int diemHienTai = (khachHang.getLoyaltyPoints() != null) ? khachHang.getLoyaltyPoints() : 0;
                    khachHang.setLoyaltyPoints(diemHienTai + diemCong);
                    customerRepository.save(khachHang);
                }
            }
        }

        // Bước 13: Trả về hóa đơn vừa thanh toán thành công
        return hoaDonDaLuu;
    }

    /**
     * BƯỚC 14 (KẾ HOẠCH TUẦN 4 - KHÔI): BÁO CÁO DOANH THU THEO NGÀY/THÁNG
     * Thống kê toàn diện doanh thu từ các hóa đơn đã thanh toán.
     */
    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(Integer days) {
        // Mặc định xem 30 ngày gần nhất nếu không chỉ định
        int soNgay = (days != null && days > 0) ? days : 30;
        LocalDateTime tuNgay = LocalDate.now().minusDays(soNgay - 1).atStartOfDay();

        List<Invoice> tatCaHoaDon = invoiceRepository.findAll();

        BigDecimal tongDoanhThu = BigDecimal.ZERO;
        long tongSoHoaDon = 0;
        BigDecimal doanhThuHomNay = BigDecimal.ZERO;

        LocalDate homNay = LocalDate.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Map gom nhóm theo ngày: Ngày -> [DoanhThu, SoHoaDon]
        Map<String, BigDecimal> doanhThuTheoNgay = new TreeMap<>();
        Map<String, Long> soHoaDonTheoNgay = new HashMap<>();

        // Khởi tạo trước các ngày trong khoảng để biểu đồ không bị khuyết ngày
        for (int i = 0; i < soNgay; i++) {
            String ngayKey = LocalDate.now().minusDays(soNgay - 1 - i).format(dtf);
            doanhThuTheoNgay.put(ngayKey, BigDecimal.ZERO);
            soHoaDonTheoNgay.put(ngayKey, 0L);
        }

        // Map gom nhóm theo phương thức thanh toán
        Map<String, BigDecimal> doanhThuTheoPhuongThuc = new LinkedHashMap<>();
        Map<String, Long> soHoaDonTheoPhuongThuc = new LinkedHashMap<>();

        for (Invoice hd : tatCaHoaDon) {
            BigDecimal tien = (hd.getTotal() != null) ? hd.getTotal() : BigDecimal.ZERO;
            tongDoanhThu = tongDoanhThu.add(tien);
            tongSoHoaDon++;

            LocalDateTime thoiGian = hd.getPaidAt();
            if (thoiGian != null) {
                // Kiểm tra nếu là hôm nay
                if (thoiGian.toLocalDate().isEqual(homNay)) {
                    doanhThuHomNay = doanhThuHomNay.add(tien);
                }

                // Nếu nằm trong khoảng số ngày lọc
                if (!thoiGian.isBefore(tuNgay)) {
                    String ngayStr = thoiGian.toLocalDate().format(dtf);
                    doanhThuTheoNgay.put(ngayStr, doanhThuTheoNgay.getOrDefault(ngayStr, BigDecimal.ZERO).add(tien));
                    soHoaDonTheoNgay.put(ngayStr, soHoaDonTheoNgay.getOrDefault(ngayStr, 0L) + 1);
                }
            }

            // Gom theo phương thức thanh toán
            String pt = (hd.getPaymentMethod() != null && !hd.getPaymentMethod().isBlank())
                    ? hd.getPaymentMethod().toUpperCase()
                    : "CASH";
            doanhThuTheoPhuongThuc.put(pt, doanhThuTheoPhuongThuc.getOrDefault(pt, BigDecimal.ZERO).add(tien));
            soHoaDonTheoPhuongThuc.put(pt, soHoaDonTheoPhuongThuc.getOrDefault(pt, 0L) + 1);
        }

        // Tính giá trị trung bình trên mỗi hóa đơn
        BigDecimal trungBinhDon = (tongSoHoaDon > 0)
                ? tongDoanhThu.divide(BigDecimal.valueOf(tongSoHoaDon), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Chuyển map ngày sang List DTO
        List<DailyRevenueItem> danhSachNgay = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : doanhThuTheoNgay.entrySet()) {
            danhSachNgay.add(new DailyRevenueItem(
                    entry.getKey(),
                    entry.getValue(),
                    soHoaDonTheoNgay.getOrDefault(entry.getKey(), 0L)
            ));
        }

        // Chuyển map phương thức thanh toán sang List DTO
        List<PaymentMethodRevenueItem> danhSachPhuongThuc = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : doanhThuTheoPhuongThuc.entrySet()) {
            danhSachPhuongThuc.add(new PaymentMethodRevenueItem(
                    entry.getKey(),
                    entry.getValue(),
                    soHoaDonTheoPhuongThuc.getOrDefault(entry.getKey(), 0L)
            ));
        }

        return new RevenueReportResponse(
                tongDoanhThu,
                tongSoHoaDon,
                trungBinhDon,
                doanhThuHomNay,
                danhSachNgay,
                danhSachPhuongThuc
        );
    }

    /**
     * BƯỚC 15: TẠO HÓA ĐƠN THỦ CÔNG
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
     * BƯỚC 16: LẤY DANH SÁCH TOÀN BỘ HÓA ĐƠN
     */
    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getAll() {
        return invoiceRepository.findAll();
    }

    /**
     * BƯỚC 17: LẤY HÓA ĐƠN CỦA 1 ĐƠN HÀNG CỤ THỂ
     */
    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getByOrderId(Integer orderId) {
        return invoiceRepository.findByOrderId(orderId);
    }

    /**
     * BƯỚC 18: TÌM HÓA ĐƠN THEO ID
     */
    @Override
    @Transactional(readOnly = true)
    public Invoice getById(Integer id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn có mã ID: " + id));
    }

    /**
     * BƯỚC 19: CẬP NHẬT HÓA ĐƠN
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
     * BƯỚC 20: XÓA HÓA ĐƠN
     */
    @Override
    public void delete(Integer id) {
        Invoice hoaDon = getById(id);
        invoiceRepository.delete(hoaDon);
    }
}
