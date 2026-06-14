package com.ticket.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Local-disk file storage — used in DEV (Windows / local development).
 * <p>
 * Files are stored under {@code storage.local.upload-dir} with UUID-based names
 * to avoid collisions. The original filename and metadata are tracked in the
 * {@code ticket_attachment} table.
 */
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path uploadDir;

    public LocalFileStorageService(@Value("${storage.local.upload-dir:./uploads}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        log.info("Local file storage initialized: {}", this.uploadDir);
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        Files.createDirectories(uploadDir);

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String storageFilename = UUID.randomUUID().toString() + extension;

        Path targetPath = uploadDir.resolve(storageFilename);
        file.transferTo(targetPath.toFile());
        log.debug("File stored: {} -> {}", originalFilename, targetPath);

        return targetPath.toString();
    }

    @Override
    public Resource load(String storageKey) {
        Path filePath = Paths.get(storageKey);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + storageKey);
        }
        return new FileSystemResource(filePath.toFile());
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path filePath = Paths.get(storageKey);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.debug("File deleted: {}", storageKey);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file: {} — {}", storageKey, e.getMessage());
        }
    }
}
