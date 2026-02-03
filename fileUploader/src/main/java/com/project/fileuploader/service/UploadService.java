package com.project.fileuploader.service;

import com.project.fileuploader.api.UploadResponse;
import com.project.fileuploader.domain.Upload;
import com.project.fileuploader.domain.UploadStatus;
import com.project.fileuploader.outbox.OutboxStatus;
import com.project.fileuploader.outbox.UploadOutbox;
import com.project.fileuploader.outbox.UploadOutboxRepository;
import com.project.fileuploader.repo.UploadRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UploadService {

    private final UploadRepository uploadRepository;
    private final UploadOutboxRepository outboxRepository;
    private final Path tmpDir;

    public UploadService(UploadRepository uploadRepository,
                         UploadOutboxRepository outboxRepository,
                         Path tmpDir) {
        this.uploadRepository = uploadRepository;
        this.outboxRepository = outboxRepository;
        this.tmpDir = tmpDir;
    }

    public UploadResponse handleUpload(String clientId, String idempotencyKey, MultipartFile file) throws IOException {
        Optional<Upload> existing = uploadRepository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey);
        if (existing.isPresent()) {
            return UploadResponse.from(existing.get());
        }

        Files.createDirectories(tmpDir);
        UUID uploadId = UUID.randomUUID();
        Path tempPath = tmpDir.resolve(uploadId.toString());

        try (var in = file.getInputStream()) {
            Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Files.deleteIfExists(tempPath);
            throw ex;
        }

        try {
            return registerUpload(clientId, idempotencyKey, file, uploadId, tempPath);
        } catch (DataIntegrityViolationException ex) {
            Files.deleteIfExists(tempPath);
            Optional<Upload> existing1 = uploadRepository.findByClientIdAndIdempotencyKey(clientId, idempotencyKey);
            if (existing1.isPresent()) {
                return UploadResponse.from(existing1.get());
            }
            throw ex;
        } catch (RuntimeException ex) {
            Files.deleteIfExists(tempPath);
            throw ex;
        }
    }

    @Transactional
    protected UploadResponse registerUpload(String clientId,
                                            String idempotencyKey,
                                            MultipartFile file,
                                            UUID uploadId,
                                            Path tempPath) {
        OffsetDateTime now = OffsetDateTime.now();

        Upload upload = new Upload();
        upload.setId(uploadId);
        upload.setClientId(clientId);
        upload.setIdempotencyKey(idempotencyKey);
        upload.setStatus(UploadStatus.RECEIVED);
        upload.setOriginalFilename(file.getOriginalFilename());
        upload.setContentType(file.getContentType());
        upload.setSizeBytes(file.getSize());
        upload.setTempPath(tempPath.toString());
        upload.setCreatedAt(now);
        upload.setUpdatedAt(now);

        uploadRepository.save(upload);

        UploadOutbox outbox = new UploadOutbox();
        outbox.setId(UUID.randomUUID());
        outbox.setUploadId(uploadId);
        outbox.setStatus(OutboxStatus.NEW);
        outbox.setCreatedAt(now);
        outboxRepository.save(outbox);

        return UploadResponse.from(upload);
    }
}