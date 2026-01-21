package com.sinik.orderapimanagement.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final NotificationProperties props;

    public EmailNotificationChannel(NotificationProperties props) {
        this.props = props;
    }

    @Override
    public String name() {
        return "email";
    }

    @Override
    public void send(NotificationMessage message) {
        // Take-home stub: configurable "to/from", and "sending" is logging.
        String to = props.email() != null ? props.email().to() : "unknown";
        String from = props.email() != null ? props.email().from() : "unknown";
        log.info("EMAIL notify from={} to={} payload={}", from, to, message);
    }
}
