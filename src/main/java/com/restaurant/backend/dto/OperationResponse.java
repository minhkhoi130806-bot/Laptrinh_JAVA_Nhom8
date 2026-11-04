package com.restaurant.backend.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record OperationResponse(Long id, String type, Map<String, String> fields, boolean done,
                                Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
