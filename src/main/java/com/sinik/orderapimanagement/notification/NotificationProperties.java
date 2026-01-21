package com.sinik.orderapimanagement.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(List<String> enabledChannels, Webhook webhook, Retry retry, Email email, Sms sms) {
    public record Webhook(String baseUrl, String path) {}
    public record Retry(int maxAttempts, long initialDelayMs, double multiplier) {}
    public record Email(String to, String from) {}
    public record Sms(String to, String from) {}
}
