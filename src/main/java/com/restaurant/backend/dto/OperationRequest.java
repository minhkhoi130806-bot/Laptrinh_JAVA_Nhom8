package com.restaurant.backend.dto;

import java.util.Map;

public record OperationRequest(Map<String, String> fields, Boolean done, Long version) {
}
