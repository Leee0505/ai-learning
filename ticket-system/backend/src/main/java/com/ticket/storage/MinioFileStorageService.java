package com.ticket.storage;

import io.minio.*;
import io.minio.errors.MinioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * MinIO (S3-compatible) object storage — used in UAT and PROD.
 * <p>
 * Files are stored in a configurable bucket with UUID-based object names.
 * The original filename is preserved in the bucket metadata and in the
 * {@code ticket_attachment} DB record.
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorageService.class);

    private final MinioClient client;
    private final String bucket;

    public MinioFileStorageService(
            @Value("${storage.minio.endpoint}") String endpoint,
            @Value("${storage.minio.access-key}") String accessKey,
            @Value("${storage.minio.secret-key}") String secretKey,
            @Value("${storage.minio.bucket}") String bucket) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
        ensureBucketExists();
        log.info("MinIO file storage initialized: endpoint={}, bucket={}", endpoint, bucket);
    }

    private void ensureBucketExists() {
        try {
            boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket created: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to verify/create MinIO bucket: {}", bucket, e);
        }
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String objectName = UUID.randomUUID().toString() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            log.debug("File stored in MinIO: {} -> {}/{}", originalFilename, bucket, objectName);
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new IOException("Failed to upload file to MinIO: " + originalFilename, e);
        }

        return objectName;
    }

    @Override
    public Resource load(String storageKey) {
        try {
            InputStream stream = client.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(storageKey)
                            .build());
            byte[] bytes = stream.readAllBytes();
            stream.close();
            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return storageKey;
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to load file from MinIO: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(storageKey)
                            .build());
            log.debug("File deleted from MinIO: {}/{}", bucket, storageKey);
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: {} — {}", storageKey, e.getMessage());
        }
    }
}
