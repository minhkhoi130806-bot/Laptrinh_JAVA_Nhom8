package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.ReceiptDetailRequest;
import com.restaurant.backend.dto.ReceiptRequest;
import com.restaurant.backend.dto.ReceiptResponse;
import com.restaurant.backend.dto.ReceiptResponse.PurchaseReportResponse;
import com.restaurant.backend.dto.ReceiptResponse.SupplierCostSummary;
import com.restaurant.backend.entity.Employee;
import com.restaurant.backend.entity.Ingredient;
import com.restaurant.backend.entity.PurchaseReceipt;
import com.restaurant.backend.entity.ReceiptDetail;
import com.restaurant.backend.entity.ReceiptDetailId;
import com.restaurant.backend.entity.Supplier;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.mapper.ReceiptMapper;
import com.restaurant.backend.repository.EmployeeRepository;
import com.restaurant.backend.repository.IngredientRepository;
import com.restaurant.backend.repository.PurchaseReceiptRepository;
import com.restaurant.backend.repository.SupplierRepository;
import com.restaurant.backend.service.ReceiptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReceiptServiceImpl implements ReceiptService {

    private final PurchaseReceiptRepository receiptRepository;
    private final SupplierRepository supplierRepository;
    private final EmployeeRepository employeeRepository;
    private final IngredientRepository ingredientRepository;
    private final ReceiptMapper mapper;

    public ReceiptServiceImpl(PurchaseReceiptRepository receiptRepository,
                              SupplierRepository supplierRepository,
                              EmployeeRepository employeeRepository,
                              IngredientRepository ingredientRepository,
                              ReceiptMapper mapper) {
        this.receiptRepository = receiptRepository;
        this.supplierRepository = supplierRepository;
        this.employeeRepository = employeeRepository;
        this.ingredientRepository = ingredientRepository;
        this.mapper = mapper;
    }

    // Lập phiếu nhập: lưu phiếu + chi tiết, đồng thời CỘNG số lượng vào tồn kho nguyên liệu
    @Override
    @Transactional
    public ReceiptResponse create(ReceiptRequest request) {
        if (request == null || request.supplierId() == null) {
            throw new BadRequestException("Vui lòng chọn nhà cung cấp");
        }
        if (request.details() == null || request.details().isEmpty()) {
            throw new BadRequestException("Phiếu nhập phải có ít nhất 1 nguyên liệu");
        }

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhà cung cấp id = " + request.supplierId()));
        Employee employee = null;
        if (request.employeeId() != null) {
            employee = employeeRepository.findById(request.employeeId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên id = " + request.employeeId()));
        }

        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.setSupplier(supplier);
        receipt.setEmployee(employee);
        receipt.setReceiptDate(request.receiptDate()); // null -> @PrePersist đặt = bây giờ
        receipt.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());

        BigDecimal total = BigDecimal.ZERO;
        Set<Long> seen = new HashSet<>();
        for (ReceiptDetailRequest line : request.details()) {
            if (line.ingredientId() == null) {
                throw new BadRequestException("Thiếu nguyên liệu trong phiếu nhập");
            }
            if (!seen.add(line.ingredientId())) {
                throw new BadRequestException("Mỗi nguyên liệu chỉ xuất hiện 1 dòng trong 1 phiếu");
            }
            if (line.quantity() == null || line.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Số lượng nhập phải lớn hơn 0");
            }
            if (line.unitPrice() == null || line.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Đơn giá nhập không hợp lệ");
            }
            Ingredient ingredient = ingredientRepository.findById(line.ingredientId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy nguyên liệu id = " + line.ingredientId()));

            ReceiptDetail detail = new ReceiptDetail();
            detail.setId(new ReceiptDetailId(null, ingredient.getId()));
            detail.setIngredient(ingredient);
            detail.setQuantity(line.quantity());
            detail.setUnitPrice(line.unitPrice());
            receipt.addDetail(detail);

            BigDecimal currentStock = ingredient.getStockQty() != null ? ingredient.getStockQty() : BigDecimal.ZERO;
            ingredient.setStockQty(currentStock.add(line.quantity()));
            ingredientRepository.save(ingredient);
            total = total.add(line.quantity().multiply(line.unitPrice()));
        }
        receipt.setTotalAmount(total);

        return mapper.toResponse(receiptRepository.save(receipt));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getAll() {
        return receiptRepository.findAllByOrderByReceiptDateDescIdDesc().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getById(Long id) {
        return mapper.toResponse(receiptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu nhập id = " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseReportResponse getPurchaseReport() {
        List<PurchaseReceipt> all = receiptRepository.findAllByOrderByReceiptDateDescIdDesc();
        BigDecimal totalSpent = BigDecimal.ZERO;
        for (PurchaseReceipt r : all) {
            if (r.getTotalAmount() != null) {
                totalSpent = totalSpent.add(r.getTotalAmount());
            }
        }
        long totalCount = all.size();

        List<Object[]> supplierAgg = receiptRepository.aggregateCostBySupplier();
        List<SupplierCostSummary> bySupplier = supplierAgg.stream().map(row -> new SupplierCostSummary(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((Number) row[2]).longValue(),
                (BigDecimal) row[3]
        )).toList();

        List<ReceiptResponse> recent = all.stream().limit(10).map(mapper::toResponse).toList();
        return new PurchaseReportResponse(totalSpent, totalCount, bySupplier, recent);
    }

    @Override
    @Transactional
    public ReceiptResponse updateNote(Long id, String auditNote) {
        PurchaseReceipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu nhập id = " + id));

        String trimmed = auditNote != null ? auditNote.trim() : "";
        if (trimmed.isBlank()) {
            throw new BadRequestException("Nội dung ghi chú kiểm toán không được để trống");
        }

        String existing = receipt.getNote();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String entry = "[" + timestamp + " - Kiểm toán/Giải trình]: " + trimmed;
        String updated = (existing == null || existing.isBlank()) ? entry : existing + "\n" + entry;

        receipt.setNote(updated);
        return mapper.toResponse(receiptRepository.save(receipt));
    }
}
