package com.sinik.orderapimanagement.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class WebhookNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotificationChannel.class);

    private final RestClient restClient;
    private final NotificationProperties props;

    public WebhookNotificationChannel(NotificationProperties props) {
        this.props = props;
        this.restClient = RestClient.create();
    }

    @Override
    public String name() {
        return "webhook";
    }

    @Retryable(
            retryFor = { ResourceAccessException.class, RestClientResponseException.class },
            maxAttemptsExpression = "#{${notification.retry.max-attempts:3}}",
            backoff = @Backoff(
                    delayExpression = "#{${notification.retry.initial-delay-ms:200}}",
                    multiplierExpression = "#{${notification.retry.multiplier:2.0}}"
            )
    )
    @Override
    public void send(NotificationMessage message) {
        String url = props.webhook().baseUrl() + props.webhook().path();

        restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(message)
                .retrieve()
                .toBodilessEntity();
    }

    @Recover
    public void recover(RestClientResponseException ex, NotificationMessage message) {
        log.warn("Webhook notification failed after retries (HTTP {}). type={} payload={}",
                ex.getStatusCode(), message.type(), message.payload());
        log.debug("Final webhook failure stacktrace", ex);
    }

    @Recover
    public void recover(ResourceAccessException ex, NotificationMessage message) {
        log.warn("Webhook notification failed after retries (connectivity). type={} payload={}",
                message.type(), message.payload());
        log.debug("Final webhook failure stacktrace", ex);
    }
}
