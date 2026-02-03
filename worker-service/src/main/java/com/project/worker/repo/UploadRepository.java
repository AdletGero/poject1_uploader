package com.project.worker.repo;

import com.project.worker.domain.Upload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


public interface UploadRepository extends JpaRepository<Upload, UUID> {
}
