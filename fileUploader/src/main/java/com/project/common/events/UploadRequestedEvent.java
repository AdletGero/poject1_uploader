package com.project.common.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UploadRequestedEvent(
        @JsonProperty("uploadId") UUID uploadId,
        @JsonProperty("clientId") String clientId,
        @JsonProperty("tempPath") String tempPath,
        @JsonProperty("originalFilename") String originalFilename,
        @JsonProperty("contentType") String contentType,
        @JsonProperty("sizeBytes") long sizeBytes,
        @JsonProperty("createdAt") OffsetDateTime createdAt
) {}
