package com.restaurant.backend.dto;

/** Minimal employee names for staff forms and historical orders. */
public record EmployeeLookupResponse(Long id, String fullName, Boolean deleted) {
}
