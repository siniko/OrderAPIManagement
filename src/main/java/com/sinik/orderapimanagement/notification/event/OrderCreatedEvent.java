package com.sinik.orderapimanagement.notification.event;

import java.util.UUID;

public record OrderCreatedEvent(UUID orderId) {}
