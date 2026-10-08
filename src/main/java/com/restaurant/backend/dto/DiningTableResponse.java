package com.restaurant.backend.dto;

import com.restaurant.backend.entity.TableStatus;

public record DiningTableResponse(
        Long id,
        String tableCode,
        Integer capacity,
        String zone,
        TableStatus status
) {
}
