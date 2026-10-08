package com.restaurant.backend.dto;

import com.restaurant.backend.entity.EmployeeRole;

import java.math.BigDecimal;

public record EmployeeRequest(
        String username,
        String fullName,
        String password,
        EmployeeRole role,
        String phone,
        BigDecimal salary
) {
}
