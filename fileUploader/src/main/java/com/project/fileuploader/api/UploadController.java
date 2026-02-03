package com.project.fileuploader.api;

import com.project.fileuploader.domain.Upload;
import com.project.fileuploader.domain.UploadStatus;
import com.project.fileuploader.repo.UploadRepository;
import com.project.fileuploader.service.UploadService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<UploadResponse> upload(
            @RequestHeader("X-Client-Id") @NotBlank String clientId,
            @RequestHeader("Idempotency-Key") @NotBlank String idemKey,
            @RequestPart("file") MultipartFile file
    ) throws Exception {

        UploadResponse response = uploadService.handleUpload(clientId, idemKey, file);
        if(response.status() == UploadStatus.STORED || response.status() == UploadStatus.FAILED){
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.accepted().body(response);
    }
}
