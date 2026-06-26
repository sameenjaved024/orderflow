package com.orderflow.notification.template;

import com.orderflow.notification.event.FulfillmentCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Email channel implementation of NotificationTemplate.
 * <p>
 * Currently logs the notification in production this would
 * be replaced with a real email provider call (SendGrid / AWS SES).
 *
 * @Component makes Spring auto-discover this class and inject it
 * into the List<NotificationTemplate> in NotificationService.
 */
@Component
@Slf4j
public class EmailNotificationTemplate implements NotificationTemplate {

    @Override
    public String channel() {
        return "EMAIL";
    }

    @Override
    public void send(FulfillmentCompletedEvent event) {
        String subject = buildSubject(event);
        String body = buildBody(event);

        log.info("[EMAIL] Sending to customerId={} | subject='{}' | body='{}'",
                event.customerId(), subject, body);

        // TODO: replace with real email provider
        // emailClient.send(event.customerId(), subject, body);
    }

    private String buildSubject(FulfillmentCompletedEvent event) {
        return String.format("Your order %s has been dispatched!", event.orderId());
    }

    private String buildBody(FulfillmentCompletedEvent event) {
        return String.format(
                "Great news! Your order %s is on its way. " +
                        "Status: %s. Thank you for shopping with OrderFlow!",
                event.orderId(), event.status()
        );
    }
}