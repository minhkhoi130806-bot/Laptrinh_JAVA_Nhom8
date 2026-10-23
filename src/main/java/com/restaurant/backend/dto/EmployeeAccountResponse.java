package com.restaurant.backend.dto;

public record EmployeeAccountResponse(
        Long employeeId,
        String username,
        String fullName,
        String role,
        String accountType
) {
}
