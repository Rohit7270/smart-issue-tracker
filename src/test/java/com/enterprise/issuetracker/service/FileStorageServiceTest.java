package com.enterprise.issuetracker.service;

import com.enterprise.issuetracker.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir);
    }

    @Test
    @DisplayName("Store and delete file physically in temporary directory")
    void deleteFile_ExistingFile_DeletesSuccessfully() throws IOException {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "Sample Content".getBytes()
        );
        String savedFileName = fileStorageService.storeFile(mockFile);
        Path physicalPath = tempDir.resolve(savedFileName);
        assertTrue(Files.exists(physicalPath), "Physical file should exist after storing");

        boolean deleted = fileStorageService.deleteFile(savedFileName);
        assertTrue(deleted, "deleteFile should return true for existing file");
        assertFalse(Files.exists(physicalPath), "Physical file should be deleted from disk");
    }

    @Test
    @DisplayName("Deleting non-existent file returns false deliberately without throwing")
    void deleteFile_NonExistentFile_ReturnsFalse() {
        boolean deleted = fileStorageService.deleteFile("missing-file-12345.txt");
        assertFalse(deleted, "deleteFile should return false for missing file");
    }

    @Test
    @DisplayName("Path traversal attempts with parent directories are rejected without deleting outside files")
    void deleteFile_PathTraversal_ThrowsBadRequestException_AndPreservesExternalFile() throws IOException {
        // Create an external file outside the upload directory (in tempDir's parent or sibling)
        Path outsideFile = Files.createTempFile("outside-sensitive-", ".txt");
        Files.writeString(outsideFile, "Sensitive system data");
        assertTrue(Files.exists(outsideFile));

        // Construct exact relative path from upload directory to the outside file
        Path uploadDir = fileStorageService.getFileStorageLocation();
        String relativeTraversalPath = uploadDir.relativize(outsideFile).toString();

        try {
            assertThrows(BadRequestException.class, () ->
                    fileStorageService.deleteFile(relativeTraversalPath)
            );
            // Verify external file was NEVER touched or deleted
            assertTrue(Files.exists(outsideFile), "External file must NOT be deleted by path traversal attempt");
        } finally {
            Files.deleteIfExists(outsideFile);
        }
    }

    @Test
    @DisplayName("Path resolving to storage root directory itself is rejected")
    void deleteFile_RootDirectoryPath_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> fileStorageService.deleteFile("."));
        assertThrows(BadRequestException.class, () -> fileStorageService.deleteFile(""));
        assertThrows(BadRequestException.class, () -> fileStorageService.deleteFile("   "));
        assertThrows(BadRequestException.class, () -> fileStorageService.deleteFile(null));
    }

    @Test
    @DisplayName("Extract stored filename from existing attachment fileUrl format")
    void extractStoredFileName_ExtractsFromUrl() {
        String url = "/api/v1/tickets/attachments/download/9e1b2390-59a6-455b-801b-c74b986cf312.pdf";
        String extracted = fileStorageService.extractStoredFileName(url);
        assertEquals("9e1b2390-59a6-455b-801b-c74b986cf312.pdf", extracted);

        String standalone = "simple-file.png";
        assertEquals("simple-file.png", fileStorageService.extractStoredFileName(standalone));

        assertNull(fileStorageService.extractStoredFileName(null));
        assertNull(fileStorageService.extractStoredFileName("   "));
    }

    @Test
    @DisplayName("deleteAttachmentFile extracts filename and deletes physical file")
    void deleteAttachmentFile_Success() throws IOException {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "photo.png", "image/png", "PNG bytes".getBytes()
        );
        String savedFileName = fileStorageService.storeFile(mockFile);
        String fileUrl = "/api/v1/tickets/attachments/download/" + savedFileName;

        boolean deleted = fileStorageService.deleteAttachmentFile(fileUrl);
        assertTrue(deleted);
        assertFalse(Files.exists(tempDir.resolve(savedFileName)));
    }
}
