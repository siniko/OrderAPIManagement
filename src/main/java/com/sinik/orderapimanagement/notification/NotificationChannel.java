package com.sinik.orderapimanagement.notification;

public interface NotificationChannel {
    String name(); // e.g. "webhook"
    void send(NotificationMessage message);
}
