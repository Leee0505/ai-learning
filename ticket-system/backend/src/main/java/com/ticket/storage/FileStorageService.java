package com.ticket.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Abstraction over file storage backends.
 * <p>
 * DEV uses local disk ({@code LocalFileStorageService}),
 * UAT/PROD use MinIO or OSS ({@code MinioFileStorageService}).
 * The active implementation is selected via {@code storage.type} property.
 */
public interface FileStorageService {

    /**
     * Store a file and return a storage identifier.
     * <p>
     * For local storage this is the absolute file path.
     * For MinIO this is the object name (bucket key).
     *
     * @param file the uploaded multipart file
     * @return a storage key that can be used later with {@link #load} and {@link #delete}
     */
    String store(MultipartFile file) throws IOException;

    /**
     * Load a file by its storage key (returned from {@link #store}).
     *
     * @param storageKey the storage identifier
     * @return a Spring Resource for streaming/downloading
     */
    Resource load(String storageKey);

    /**
     * Delete a file by its storage key.
     *
     * @param storageKey the storage identifier
     */
    void delete(String storageKey);
}
