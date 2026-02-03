package com.project.worker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.event.ConsumerStartedEvent;
import org.springframework.kafka.event.ConsumerStoppedEvent;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.stereotype.Component;

@Component
public class KafkaListenerLoggingConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaListenerLoggingConfig.class);

    @EventListener
    public void onConsumerStarted(ConsumerStartedEvent event) {
        logger.info("Kafka consumer started. event={}", event);
    }

    @EventListener
    public void onConsumerStopped(ConsumerStoppedEvent event) {
        logger.warn("Kafka consumer stopped. event={}", event);
    }

    @EventListener
    public void onContainerIdle(ListenerContainerIdleEvent event) {
        logger.info("Kafka listener idle. id={}, idleTime={}, topicPartitions={}", event.getListenerId(), event.getIdleTime(), event.getTopicPartitions());
    }
}