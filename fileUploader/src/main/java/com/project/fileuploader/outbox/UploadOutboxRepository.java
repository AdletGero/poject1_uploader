package com.project.fileuploader.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UploadOutboxRepository extends JpaRepository<UploadOutbox, UUID> {
    org.springframework.data.domain.Page<UploadOutbox> findByStatusOrderByCreatedAt(OutboxStatus status, org.springframework.data.domain.Pageable pageable);
}