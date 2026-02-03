package com.project.worker.service;

import com.project.common.events.UploadJobEvent;
import com.project.worker.domain.Upload;
import com.project.worker.domain.UploadStatus;
import com.project.worker.repo.UploadRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

@Service
public class UploadWorker {
    private final UploadRepository uploadRepository;
    private final MinioClient minioClient;
    private final String bucket;
    public UploadWorker(UploadRepository uploadRepository, MinioClient minioClient,
                        @Value("${app.storage.bucket}") String bucket) {
        this.uploadRepository = uploadRepository;
        this.minioClient = minioClient;
        this.bucket = bucket;
    }
    @KafkaListener(topics = "${app.kafka.topic-upload-jobs}")
    @Transactional
    public void handle(UploadJobEvent event) {
        Upload upload = uploadRepository.findById(event.uploadId()).orElse(null);
        if(upload == null) {
            return;
        }
        if (upload.getStatus() == UploadStatus.STORED || upload.getStatus() == UploadStatus.FAILED) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        upload.setStatus(UploadStatus.UPLOADING);
        upload.setCreatedAt(now);

        Path tempPath = Path.of(upload.getTempPath());
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

        } catch (Exception ex){
            upload.setStatus(UploadStatus.FAILED);
            upload.setErrorMessage(ex.getMessage());
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
