package com.restaurant.backend.dto;

import com.restaurant.backend.entity.EmployeeRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Không trả password về client
public record EmployeeResponse(
        Long id,
        String username,
        String fullName,
        EmployeeRole role,
        String phone,
        BigDecimal salary,
        Boolean deleted,
        LocalDateTime deletedAt
) {
}
