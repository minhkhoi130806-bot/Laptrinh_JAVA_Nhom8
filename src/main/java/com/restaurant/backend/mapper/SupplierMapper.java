package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.SupplierResponse;
import com.restaurant.backend.entity.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

    public SupplierResponse toResponse(Supplier s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getPhone(), s.getEmail(), s.getAddress());
    }
}
