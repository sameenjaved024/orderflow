package com.orderflow.notification.service;

import com.orderflow.notification.event.FulfillmentCompletedEvent;
import com.orderflow.notification.template.NotificationTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

    @Mock
    private NotificationTemplate emailTemplate;
    @Mock
    private NotificationTemplate smsTemplate;

    @Nested
    @DisplayName("notifyAllChannels")
    class NotifyAllChannels {

        @Test
        @DisplayName("should dispatch notification to every registered channel")
        void shouldDispatchToAllChannels() {
            // GIVEN
            NotificationService service = new NotificationService(
                    List.of(emailTemplate, smsTemplate));
            FulfillmentCompletedEvent event = buildEvent();

            // WHEN
            service.notifyAllChannels(event);

            // THEN — both channels must receive the event
            then(emailTemplate).should().send(event);
            then(smsTemplate).should().send(event);
        }

        @Test
        @DisplayName("should continue dispatching when one channel fails")
        void shouldContinueWhenOneChannelFails() {
            // GIVEN — email throws, SMS must still fire
            NotificationService service = new NotificationService(
                    List.of(emailTemplate, smsTemplate));
            FulfillmentCompletedEvent event = buildEvent();

            willThrow(new RuntimeException("Email provider unavailable"))
                    .given(emailTemplate).send(event);

            // WHEN
            service.notifyAllChannels(event);

            // THEN — SMS still sent despite email failure
            then(smsTemplate).should().send(event);
        }

        @Test
        @DisplayName("should not dispatch when no channels are registered")
        void shouldHandleEmptyChannelList() {
            // GIVEN — no channels registered
            NotificationService service = new NotificationService(List.of());
            FulfillmentCompletedEvent event = buildEvent();

            // WHEN / THEN — must not throw
            service.notifyAllChannels(event);

            then(emailTemplate).should(never()).send(any());
            then(smsTemplate).should(never()).send(any());
        }
    }

    // ── Builder ───────────────────────────────────────────────────

    private FulfillmentCompletedEvent buildEvent() {
        return new FulfillmentCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "cust-001",
                "DISPATCHED",
                Instant.now()
        );
    }
}