package com.restaurant.backend.controller;

import com.restaurant.backend.dto.CheckoutRequest;
import com.restaurant.backend.dto.InvoiceRequest;
import com.restaurant.backend.dto.InvoiceResponse;
import com.restaurant.backend.entity.Invoice;
import com.restaurant.backend.mapper.InvoiceMapper;
import com.restaurant.backend.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================
 * CONTROLLER QUẢN LÝ HÓA ĐƠN & THANH TOÁN (/api/invoices)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Nghiệp vụ Thanh toán Hóa đơn)
 * 
 * Mục đích:
 * Tiếp nhận các yêu cầu HTTP liên quan đến Hóa đơn và Thanh toán tiền:
 * 1. POST   /api/invoices/checkout : API thanh toán đơn hàng (Tự động tính tiền, tạo Invoice, đổi status đơn sang PAID, giải phóng bàn)
 * 2. POST   /api/invoices          : Tạo hóa đơn thủ công
 * 3. GET    /api/invoices          : Lấy danh sách tất cả hóa đơn (hoặc lọc theo orderId)
 * 4. GET    /api/invoices/{id}     : Xem chi tiết một hóa đơn theo ID
 * 5. PUT    /api/invoices/{id}     : Cập nhật thông tin hóa đơn
 * 6. DELETE /api/invoices/{id}     : Xóa hóa đơn
 * =========================================================================
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceMapper invoiceMapper;

    // Tiêm Service và Mapper qua Constructor
    public InvoiceController(
            InvoiceService invoiceService,
            InvoiceMapper invoiceMapper) {
        this.invoiceService = invoiceService;
        this.invoiceMapper = invoiceMapper;
    }

    /**
     * API 1 (TRỌNG TÂM TUẦN 3): THANH TOÁN ĐƠN HÀNG VÀ TẠO HÓA ĐƠN (@Transactional)
     * Endpoint: POST /api/invoices/checkout
     * Body gửi lên:
     * {
     *   "orderId": 1,
     *   "discount": 10000,
     *   "paymentMethod": "CASH"
     * }
     */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        // Gọi Service thực hiện quy trình thanh toán trọn gói
        Invoice hoaDon = invoiceService.checkout(request);

        // Chuyển kết quả Entity thành DTO Response trả về
        return invoiceMapper.toResponse(hoaDon);
    }

    /**
     * API 2: Tạo hóa đơn thủ công
     * Endpoint: POST /api/invoices
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@RequestBody InvoiceRequest request) {
        Invoice entity = invoiceMapper.toEntity(request);
        Invoice hoaDonMoi = invoiceService.create(entity);
        return invoiceMapper.toResponse(hoaDonMoi);
    }

    /**
     * API 3: Lấy danh sách hóa đơn
     * Endpoint: GET /api/invoices hoặc GET /api/invoices?orderId=1
     */
    @GetMapping
    public List<InvoiceResponse> getAll(@RequestParam(required = false) Integer orderId) {
        List<Invoice> danhSachHoaDon;

        // Nếu có orderId thì lấy hóa đơn của đơn đó, ngược lại lấy tất cả
        if (orderId != null) {
            danhSachHoaDon = invoiceService.getByOrderId(orderId);
        } else {
            danhSachHoaDon = invoiceService.getAll();
        }

        // Chuyển đổi danh sách Entity sang danh sách DTO Response
        List<InvoiceResponse> ketQua = new ArrayList<>();
        for (Invoice h : danhSachHoaDon) {
            ketQua.add(invoiceMapper.toResponse(h));
        }

        return ketQua;
    }

    /**
     * API 4: Tìm hóa đơn theo ID
     * Endpoint: GET /api/invoices/{id}
     */
    @GetMapping("/{id}")
    public InvoiceResponse getById(@PathVariable Integer id) {
        Invoice hoaDon = invoiceService.getById(id);
        return invoiceMapper.toResponse(hoaDon);
    }

    /**
     * API 5: Cập nhật hóa đơn
     * Endpoint: PUT /api/invoices/{id}
     */
    @PutMapping("/{id}")
    public InvoiceResponse update(
            @PathVariable Integer id,
            @RequestBody InvoiceRequest request) {

        Invoice entity = invoiceMapper.toEntity(request);
        Invoice hoaDonCapNhat = invoiceService.update(id, entity);
        return invoiceMapper.toResponse(hoaDonCapNhat);
    }

    /**
     * API 6: Xóa hóa đơn
     * Endpoint: DELETE /api/invoices/{id}
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        invoiceService.delete(id);
    }
}
