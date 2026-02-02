package com.project.fileuploader.api;

import com.project.fileuploader.domain.Upload;
import com.project.fileuploader.domain.UploadStatus;
import com.project.fileuploader.repo.UploadRepository;
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

    private final UploadRepository uploadRepository;
    private final Path tmpDir;

    public UploadController(UploadRepository uploadRepository,
                            @Value("${app.tmp-dir}") String tmpDir) {
        this.uploadRepository = uploadRepository;
        this.tmpDir = Paths.get(tmpDir);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<UploadResponse> upload(
            @RequestHeader("X-Client-Id") @NotBlank String clientId,
            @RequestHeader("Idempotency-Key") @NotBlank String idemKey,
            @RequestPart("file") MultipartFile file
    ) throws Exception {

        Files.createDirectories(tmpDir);

        UUID uploadId = UUID.randomUUID();
        Path tempPath = tmpDir.resolve(uploadId.toString());

        try (var in = file.getInputStream()) {
            Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
        }

        OffsetDateTime now = OffsetDateTime.now();

        Upload u = new Upload();
        u.setId(uploadId);
        u.setClientId(clientId);
        u.setIdempotencyKey(idemKey);
        u.setStatus(UploadStatus.RECEIVED);
        u.setOriginalFilename(file.getOriginalFilename());
        u.setContentType(file.getContentType());
        u.setSizeBytes(file.getSize());
        u.setTempPath(tempPath.toString());
        u.setCreatedAt(now);
        u.setUpdatedAt(now);

        uploadRepository.save(u);

        return ResponseEntity.accepted().body(new UploadResponse(uploadId));
    }
}
