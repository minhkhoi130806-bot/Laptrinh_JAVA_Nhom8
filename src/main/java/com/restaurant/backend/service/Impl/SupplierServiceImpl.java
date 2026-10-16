package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.SupplierRequest;
import com.restaurant.backend.dto.SupplierResponse;
import com.restaurant.backend.entity.Supplier;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.mapper.SupplierMapper;
import com.restaurant.backend.repository.SupplierRepository;
import com.restaurant.backend.service.SupplierService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper mapper;

    public SupplierServiceImpl(SupplierRepository supplierRepository, SupplierMapper mapper) {
        this.supplierRepository = supplierRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        validate(request);
        Supplier supplier = new Supplier();
        apply(supplier, request);
        return mapper.toResponse(supplierRepository.save(supplier));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> getAll() {
        return supplierRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getById(Long id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        validate(request);
        Supplier existing = find(id);
        apply(existing, request);
        return mapper.toResponse(supplierRepository.save(existing));
    }

    // Nhà cung cấp đã có phiếu nhập sẽ bị DB chặn -> GlobalExceptionHandler trả 409
    @Override
    @Transactional
    public void delete(Long id) {
        supplierRepository.delete(find(id));
        supplierRepository.flush();
    }

    private Supplier find(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhà cung cấp id = " + id));
    }

    private void apply(Supplier s, SupplierRequest r) {
        s.setName(r.name().trim());
        s.setPhone(r.phone().trim());
        s.setEmail(r.email() == null || r.email().isBlank() ? null : r.email().trim());
        s.setAddress(r.address() == null || r.address().isBlank() ? null : r.address().trim());
    }

    private void validate(SupplierRequest r) {
        if (r == null || r.name() == null || r.name().isBlank()) {
            throw new BadRequestException("Tên nhà cung cấp không được để trống");
        }
        if (r.phone() == null || r.phone().isBlank()) {
            throw new BadRequestException("Số điện thoại không được để trống");
        }
    }
}
