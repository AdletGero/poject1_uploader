package com.project.worker.service;

import com.project.common.events.UploadJobEvent;
import com.project.worker.domain.Upload;
import com.project.worker.domain.UploadStatus;
import com.project.worker.repo.UploadRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

@Service
public class UploadWorker {
    private static final Logger logger = LoggerFactory.getLogger(UploadWorker.class);
    private final UploadRepository uploadRepository;
    private final MinioClient minioClient;
    private final String bucket;
    private final String topic;
    public UploadWorker(UploadRepository uploadRepository, MinioClient minioClient,
                        @Value("${app.storage.bucket}") String bucket,
                        @Value("${app.kafka.topic-upload-jobs}") String topic) {
        this.uploadRepository = uploadRepository;
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.topic = topic;
    }
    @PostConstruct
    public void logInitialized() {
        logger.info("UploadWorker initialized. bucket={}, topic={}", bucket, topic);
    }
    @KafkaListener(
            id = "upload-worker",
            topics = "${app.kafka.topic-upload-jobs}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handle(ConsumerRecord<String, UploadJobEvent> record) {
        UploadJobEvent event = record.value();
        logger.info(
                "Received upload job event. topic={}, partition={}, offset={}, key={}, uploadId={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                event == null ? null : event.uploadId()
        );
        if (event == null) {
            logger.warn(
                    "Upload job event payload is null. topic={}, partition={}, offset={}, key={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key()
            );
            return;
        }

        logger.info("Received upload job event. uploadId={}", event.uploadId());
        Upload upload = uploadRepository.findById(event.uploadId()).orElse(null);
        if(upload == null) {
            logger.warn("Upload not found for event. uploadId={}", event.uploadId());
            return;
        }
        if (upload.getStatus() == UploadStatus.STORED || upload.getStatus() == UploadStatus.FAILED) {
            logger.info(
                    "Upload already finalized. uploadId={}, status={}, storageKey={}, tempPath={}",
                    upload.getId(),
                    upload.getStatus(),
                    upload.getStorageKey(),
                    upload.getTempPath()
            );
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        upload.setStatus(UploadStatus.UPLOADING);
        upload.setCreatedAt(now);

        Path tempPath = Path.of(upload.getTempPath());
        logger.info(
                "Processing upload file. uploadId={}, filename={}, contentType={}, sizeBytes={}, tempPath={}, exists={}",
                upload.getId(),
                upload.getOriginalFilename(),
                upload.getContentType(),
                upload.getSizeBytes(),
                tempPath,
                Files.exists(tempPath)
        );
        try{
            ensureBucket();
            String storageKey = upload.getId().toString();
            minioClient.uploadObject(
                    UploadObjectArgs.builder().
                            bucket(bucket).
                            object(storageKey).
                            filename(tempPath.toString()).
                            build()
            );
            upload.setStorageKey(storageKey);
            upload.setStatus(UploadStatus.STORED);
            upload.setErrorMessage(null);
            logger.info(
                    "Upload stored successfully. uploadId={}, storageKey={}, bucket={}",
                    upload.getId(),
                    storageKey,
                    bucket
            );

        } catch (Exception ex){
            upload.setStatus(UploadStatus.FAILED);
            upload.setErrorMessage(truncateError(ex.getMessage()));
            logger.error(
                    "Upload failed. uploadId={}, bucket={}, tempPath={}",
                    upload.getId(),
                    bucket,
                    tempPath,
                    ex
            );
        } finally {
            try{
                Files.deleteIfExists(tempPath);
            } catch (Exception ignored){}
            upload.setUpdatedAt(OffsetDateTime.now());
        }
    }

    private void ensureBucket() throws Exception{
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if(!exists){
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String truncateError(String message){
        if(message == null || message.isEmpty()){
            return null;
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }


}
