package com.sinik.orderapimanagement.notification.event;

import com.sinik.orderapimanagement.domain.OrderStatus;
import java.util.UUID;

public record OrderStatusChangedEvent(UUID orderId, OrderStatus from, OrderStatus to) {}
