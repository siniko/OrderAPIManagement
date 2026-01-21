package com.sinik.orderapimanagement.notification;

import java.time.Instant;
import java.util.Map;

public record NotificationMessage(
        String type,        // ORDER_CREATED / ORDER_STATUS_CHANGED
        Instant occurredAt,
        Map<String, Object> payload
) {}
