package com.restaurant.backend.dto;

import com.restaurant.backend.entity.TableStatus;

import java.time.LocalDateTime;

public record DiningTableResponse(
        Long id,
        String tableCode,
        Integer capacity,
        String zone,
        TableStatus status,
        Boolean deleted,
        LocalDateTime deletedAt
) {
}
