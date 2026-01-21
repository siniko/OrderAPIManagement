package com.sinik.orderapimanagement.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationRouter {
    private static final Logger log = LoggerFactory.getLogger(NotificationRouter.class);

    private final NotificationProperties props;
    private final Map<String, NotificationChannel> channelsByName;

    public NotificationRouter(NotificationProperties props, List<NotificationChannel> channels) {
        this.props = props;
        this.channelsByName = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::name, c -> c));
    }

    public void notifyAllEnabled(NotificationMessage message) {
        for (String channelName : props.enabledChannels()) {
            NotificationChannel channel = channelsByName.get(channelName);
            if (channel == null) {
                log.warn("Notification channel '{}' is enabled but no bean exists", channelName);
                continue;
            }
            try {
                channel.send(message);
            } catch (Exception ex) {
                log.error("Notification channel '{}' failed", channelName, ex);
            }
        }
    }
}
