package com.sinik.orderapimanagement.api.dto;

import com.sinik.orderapimanagement.domain.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        String customerId,
        Instant createdAt,
        Instant updatedAt
) {}
