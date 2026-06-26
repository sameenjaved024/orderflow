package com.orderflow.notification.service;

import com.orderflow.notification.event.FulfillmentCompletedEvent;
import com.orderflow.notification.template.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dispatches notifications through every registered channel.
 *
 * Design "Strategy pattern":
 * Spring injects ALL beans implementing NotificationTemplate into this list.
 * Currently that is EmailNotificationTemplate and SmsNotificationTemplate.
 * Adding a new channel requires only a new @Component this class never changes.
 *
 * Failure isolation:
 * If one channel fails (e.g. email provider is down), the catch block
 * prevents it from stopping other channels. SMS will still send even
 * if email throws an exception.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final List<NotificationTemplate> notificationTemplates;

    public void notifyAllChannels(FulfillmentCompletedEvent event) {
        log.info("Dispatching notifications for orderId={} via {} channel(s)",
                event.orderId(), notificationTemplates.size());

        notificationTemplates.forEach(template -> {
            try {
                template.send(event);
                log.info("Notification sent via {} for orderId={}",
                        template.channel(), event.orderId());
            } catch (Exception ex) {
                log.error("Failed to send {} notification for orderId={}: {}",
                        template.channel(), event.orderId(), ex.getMessage(), ex);
            }
        });
    }
}