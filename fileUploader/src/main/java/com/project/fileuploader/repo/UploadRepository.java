package com.project.fileuploader.repo;

import com.project.fileuploader.domain.Upload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UploadRepository extends JpaRepository<Upload, UUID> {
    Optional<Upload> findByClientIdAndIdempotencyKey(String clientId, String idempotencyKey);
}
