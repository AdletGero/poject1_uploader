package com.project.fileuploader.api;

import com.project.fileuploader.domain.Upload;
import com.project.fileuploader.domain.UploadStatus;

import java.util.UUID;

public record UploadResponse(UUID uploadId, UploadStatus status) {
    public static UploadResponse from(Upload upload) {
        return new UploadResponse(upload.getId(), upload.getStatus());
    }
}
