package com.project.fileuploader.outbox;

import com.project.common.events.UploadJobEvent;
import com.project.fileuploader.domain.Upload;
import com.project.fileuploader.domain.UploadStatus;
import com.project.fileuploader.repo.UploadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class UploadOutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(UploadOutboxPublisher.class);

    private final UploadOutboxRepository outboxRepository;
    private final UploadRepository uploadRepository;
    private final KafkaTemplate<String, UploadJobEvent> kafkaTemplate;
    private final String topic;
    private final int batchSize;

    public UploadOutboxPublisher(UploadOutboxRepository outboxRepository,
                                 UploadRepository uploadRepository,
                                 KafkaTemplate<String, UploadJobEvent> kafkaTemplate,
                                 @Value("${app.kafka.topic-upload-jobs}") String topic,
                                 @Value("${app.outbox.batch-size:50}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.uploadRepository = uploadRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        List<UploadOutbox> batch = outboxRepository
                .findByStatusOrderByCreatedAt(OutboxStatus.NEW, PageRequest.of(0, batchSize))
                .getContent();
        if (batch.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        for (UploadOutbox outbox : batch) {
            Upload upload = uploadRepository.findById(outbox.getUploadId()).orElse(null);
            if (upload == null) {
                outbox.setStatus(OutboxStatus.FAILED);
                outbox.setSentAt(now);
                continue;
            }
            if (upload.getStatus() == UploadStatus.RECEIVED) {
                upload.setStatus(UploadStatus.UPLOADING);
                upload.setUpdatedAt(now);
            }
            try {
                logger.info(
                        "Publishing upload job event. topic={}, uploadId={}, outboxId={}, status={}, filename={}, contentType={}, sizeBytes={}, tempPath={}",
                        topic,
                        outbox.getUploadId(),
                        outbox.getId(),
                        upload.getStatus(),
                        upload.getOriginalFilename(),
                        upload.getContentType(),
                        upload.getSizeBytes(),
                        upload.getTempPath()
                );
                kafkaTemplate.send(topic, outbox.getUploadId().toString(), new UploadJobEvent(outbox.getUploadId()))
                        .get();
                logger.info(
                        "Published upload job event. topic={}, uploadId={}, outboxId={}",
                        topic,
                        outbox.getUploadId(),
                        outbox.getId()
                );
                outbox.setStatus(OutboxStatus.SENT);
                outbox.setSentAt(now);
            } catch (Exception ex) {
                logger.error(
                        "Failed to publish upload job event. topic={}, uploadId={}, outboxId={}, tempPath={}",
                        topic,
                        outbox.getUploadId(),
                        outbox.getId(),
                        upload.getTempPath(),
                        ex
                );
                outbox.setStatus(OutboxStatus.FAILED);
                outbox.setSentAt(now);
                upload.setStatus(UploadStatus.FAILED);
                upload.setErrorMessage(truncateError(ex.getMessage()));
                upload.setUpdatedAt(now);
                cleanupTemp(upload.getTempPath());
            }
        }
    }

    private void cleanupTemp(String tempPath) {
        if (tempPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(tempPath));
        } catch (Exception ignored) {
        }
    }

    private String truncateError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}