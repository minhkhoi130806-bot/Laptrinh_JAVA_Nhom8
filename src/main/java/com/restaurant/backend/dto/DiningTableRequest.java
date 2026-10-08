package com.restaurant.backend.dto;

import com.restaurant.backend.entity.TableStatus;

public record DiningTableRequest(
        String tableCode,
        Integer capacity,
        String zone,
        TableStatus status
) {
}
