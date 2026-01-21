package com.sinik.orderapimanagement.error;

import com.sinik.orderapimanagement.domain.OrderStatus;
import java.util.UUID;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(UUID id, OrderStatus from, OrderStatus to) {
        super("Invalid status transition for order " + id + ": " + from + " -> " + to);
    }
}
