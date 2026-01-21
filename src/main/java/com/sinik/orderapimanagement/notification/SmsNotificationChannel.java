package com.sinik.orderapimanagement.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsNotificationChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(SmsNotificationChannel.class);

    private final NotificationProperties props;

    public SmsNotificationChannel(NotificationProperties props) {
        this.props = props;
    }

    @Override
    public String name() {
        return "sms";
    }

    @Override
    public void send(NotificationMessage message) {
        // Take-home stub: configurable "to/from", and "sending" is logging.
        String to = props.sms() != null ? props.sms().to() : "unknown";
        String from = props.sms() != null ? props.sms().from() : "unknown";
        log.info("SMS notify from={} to={} payload={}", from, to, message);
    }
}
