package com.orderflow.notification.template;

import com.orderflow.notification.event.FulfillmentCompletedEvent;

/**
 * Strategy pattern -> defines the contract for all notification channels.
 * <p>
 * Why an interface here:
 * Each notification channel (email, SMS, push) implements this interface.
 * NotificationService holds a List<NotificationTemplate> injected by Spring.
 * Adding a new channel requires only a new @Component, zero changes to
 * NotificationService or any existing template.
 * <p>
 * This is the Open/Closed principle in action:
 * open for extension, closed for modification.
 */
public interface NotificationTemplate {

    /**
     * Returns the channel name this template handles.
     * Used in logs to identify which channel sent the notification.
     */
    String channel();

    /**
     * Sends a notification for the given fulfillment event.
     */
    void send(FulfillmentCompletedEvent event);
}