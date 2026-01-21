package com.sinik.orderapimanagement.notification;

import com.sinik.orderapimanagement.notification.event.OrderCreatedEvent;
import com.sinik.orderapimanagement.notification.event.OrderStatusChangedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.Map;

@Component
public class OrderNotificationListener {

    private final NotificationRouter router;

    public OrderNotificationListener(NotificationRouter router) {
        this.router = router;
    }

    @Async
    @TransactionalEventListener
    public void onOrderCreated(OrderCreatedEvent e) {
        router.notifyAllEnabled(new NotificationMessage(
                "ORDER_CREATED",
                Instant.now(),
                Map.of("orderId", e.orderId())
        ));
    }

    @Async
    @TransactionalEventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent e) {
        router.notifyAllEnabled(new NotificationMessage(
                "ORDER_STATUS_CHANGED",
                Instant.now(),
                Map.of(
                        "orderId", e.orderId(),
                        "from", e.from().name(),
                        "to", e.to().name()
                )
        ));
    }
}
