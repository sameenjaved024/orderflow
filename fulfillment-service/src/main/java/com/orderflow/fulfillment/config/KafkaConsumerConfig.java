package com.orderflow.fulfillment.config;

import com.orderflow.fulfillment.event.OrderCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * ConsumerFactory — creates Kafka consumer instances.
     * <p>
     * Key settings:
     * GROUP_ID = "fulfillment-group" — all instances of fulfillment-service
     * share this group. Kafka distributes partitions across them automatically.
     * If you run 3 instances and the topic has 3 partitions, each instance
     * gets exactly one partition.
     * <p>
     * AUTO_OFFSET_RESET = "earliest" — on first startup, read from the
     * beginning of the topic. This means if fulfillment-service was down
     * when order-service published events, it will catch up on restart.
     * <p>
     * ENABLE_AUTO_COMMIT = false — disables automatic offset commit.
     * We commit manually after successful processing only.
     */
    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> orderEventConsumerFactory() {
        JsonDeserializer<OrderCreatedEvent> deserializer =
                new JsonDeserializer<>(OrderCreatedEvent.class, false);

        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG, "fulfillment-group",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                        ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10
                ),
                new StringDeserializer(),
                deserializer
        );
    }

    /**
     * ConcurrentKafkaListenerContainerFactory — wraps the consumer factory
     * and adds Spring-level features like manual ACK mode and concurrency.
     * <p>
     * AckMode.MANUAL_IMMEDIATE — the listener must call acknowledgment.acknowledge()
     * explicitly. Spring will not commit the offset until that call happens.
     * <p>
     * setConcurrency(3) — creates 3 consumer threads per service instance.
     * Each thread reads from one Kafka partition independently.
     * This means one fulfillment-service instance can process 3 partitions
     * simultaneously without needing multiple deployments.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(orderEventConsumerFactory());
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(3);
        return factory;
    }
}