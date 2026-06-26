package com.orderflow.notification.template;

import com.orderflow.notification.event.FulfillmentCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SMS channel implementation of NotificationTemplate.
 * <p>
 * Currently logs the notification in production this would
 * be replaced with a real SMS provider call (Twilio / AWS SNS).
 * <p>
 * Adding this channel required zero changes to NotificationService
 * or EmailNotificationTemplate this is the Strategy pattern benefit.
 */
@Component
@Slf4j
public class SmsNotificationTemplate implements NotificationTemplate {

    @Override
    public String channel() {
        return "SMS";
    }

    @Override
    public void send(FulfillmentCompletedEvent event) {
        String message = buildMessage(event);

        log.info("[SMS] Sending to customerId={} | message='{}'",
                event.customerId(), message);

        // TODO: replace with real SMS provider
        // twilioClient.sendSms(event.customerId(), message);
    }

    private String buildMessage(FulfillmentCompletedEvent event) {
        return String.format(
                "OrderFlow: Your order %s has been dispatched! Status: %s",
                event.orderId(), event.status()
        );
    }
}