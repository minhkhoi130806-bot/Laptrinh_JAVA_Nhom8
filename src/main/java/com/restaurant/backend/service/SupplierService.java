package com.restaurant.backend.service;

import com.restaurant.backend.dto.SupplierRequest;
import com.restaurant.backend.dto.SupplierResponse;

import java.util.List;

public interface SupplierService {
    SupplierResponse create(SupplierRequest request);
    List<SupplierResponse> getAll();
    SupplierResponse getById(Long id);
    SupplierResponse update(Long id, SupplierRequest request);
    void delete(Long id);
}
