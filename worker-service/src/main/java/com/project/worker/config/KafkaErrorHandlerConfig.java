package com.project.worker.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaErrorHandlerConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler((ConsumerRecord<?, ?> record, Exception ex) -> {
            if (record == null) {
                logger.error("Kafka listener error with no record.", ex);
                return;
            }
            logger.error(
                    "Kafka listener error. topic={}, partition={}, offset={}, key={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    ex
            );
        }, new FixedBackOff(0L, 0L));
        handler.setAckAfterHandle(false);
        return handler;
    }
}