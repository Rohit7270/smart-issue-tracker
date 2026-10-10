package com.enterprise.issuetracker.service;

import com.enterprise.issuetracker.exception.BadRequestException;
import com.enterprise.issuetracker.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Service for managing local disk file uploads and downloads.
 * Uses Java NIO Paths for safe cross-platform file handling.
 */
@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;

    @Autowired
    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this(Paths.get(uploadDir));
    }

    public FileStorageService() {
        this(Paths.get("uploads"));
    }

    public FileStorageService(Path storagePath) {
        this.fileStorageLocation = storagePath.toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("File storage directory initialized at: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            log.error("Could not create the directory where uploaded files will be stored", ex);
            throw new RuntimeException("Could not create the storage directory for uploaded files.", ex);
        }
    }

    public Path getFileStorageLocation() {
        return this.fileStorageLocation;
    }

    /**
     * Resolves and normalizes a file name relative to the storage directory,
     * ensuring it stays strictly inside the configured upload directory.
     * Rejects path traversal sequences and paths that resolve to the storage directory itself.
     *
     * @param fileName the relative file name
     * @return the resolved and validated Path
     * @throws BadRequestException if the path is invalid or traverses outside
     */
    public Path validateAndResolvePath(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new BadRequestException("File name must not be blank.");
        }
        Path filePath = this.fileStorageLocation.resolve(fileName.trim()).normalize();
        if (!filePath.startsWith(this.fileStorageLocation) || filePath.equals(this.fileStorageLocation)) {
            throw new BadRequestException("Invalid file path: path traversal detected or path resolves to upload directory root.");
        }
        return filePath;
    }

    /**
     * Extracts the stored physical file name from an attachment's fileUrl.
     * Handles URLs in format '/api/v1/tickets/attachments/download/{fileName}'
     * as well as standalone file names.
     *
     * @param fileUrl the attachment file URL or path
     * @return the extracted file name, or null if input is empty
     */
    public String extractStoredFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return null;
        }
        String cleanUrl = fileUrl.trim();
        int queryIdx = cleanUrl.indexOf('?');
        if (queryIdx >= 0) {
            cleanUrl = cleanUrl.substring(0, queryIdx);
        }
        int lastSlash = Math.max(cleanUrl.lastIndexOf('/'), cleanUrl.lastIndexOf('\\'));
        return (lastSlash >= 0) ? cleanUrl.substring(lastSlash + 1) : cleanUrl;
    }

    /**
     * Stores an uploaded multipart file securely in local storage.
     *
     * @param file the uploaded file
     * @return the unique generated filename under which the file is saved
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Failed to upload empty file.");
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        try {
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully saved file '{}' as '{}'", originalFileName, uniqueFileName);
            return uniqueFileName;
        } catch (IOException ex) {
            log.error("Failed to store file '{}'", originalFileName, ex);
            throw new RuntimeException("Failed to store file " + originalFileName, ex);
        }
    }

    /**
     * Loads a stored file as a Spring Resource for download.
     *
     * @param fileName the unique filename
     * @return Resource representing the file
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = validateAndResolvePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new BadRequestException("Invalid file path for file: " + fileName);
        }
    }

    /**
     * Deletes a stored file from the upload directory by file name.
     *
     * @param fileName the unique stored file name
     * @return true if the file existed and was deleted, false if the file was already missing
     * @throws BadRequestException if the path is invalid or attempts path traversal
     * @throws RuntimeException if an unexpected I/O failure occurs
     */
    public boolean deleteFile(String fileName) {
        Path filePath = validateAndResolvePath(fileName);
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Successfully deleted stored file: '{}'", filePath.getFileName());
            } else {
                log.warn("Stored file '{}' was not found on disk during deletion", filePath.getFileName());
            }
            return deleted;
        } catch (IOException ex) {
            log.error("Unexpected I/O failure while deleting file '{}'", filePath, ex);
            throw new RuntimeException("Unexpected I/O failure while deleting file: " + fileName, ex);
        }
    }

    /**
     * Deletes a stored file given an attachment's fileUrl.
     *
     * @param fileUrl the attachment file URL
     * @return true if deleted, false if missing or invalid URL
     */
    public boolean deleteAttachmentFile(String fileUrl) {
        String storedFileName = extractStoredFileName(fileUrl);
        if (storedFileName == null || storedFileName.trim().isEmpty()) {
            log.warn("Cannot delete attachment file: fileUrl is empty or invalid: '{}'", fileUrl);
            return false;
        }
        return deleteFile(storedFileName);
    }
}
