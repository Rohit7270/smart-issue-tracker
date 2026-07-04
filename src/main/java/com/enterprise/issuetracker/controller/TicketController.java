package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.*;
import com.enterprise.issuetracker.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Ticket Management", description = "Endpoints for creating, updating, commenting, uploading files, and listing issues")
@SecurityRequirement(name = "Bearer Authentication")
public class TicketController {

    private final TicketService ticketService;
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    @Autowired
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @PostMapping
    @Operation(summary = "Create a new ticket", description = "Reporter is resolved automatically from the authenticated JWT session.")
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody TicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TicketResponse created = ticketService.createTicket(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Ticket created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket by ID", description = "Retrieves ticket details including categories, reporter, assignee, comments, and attachments.")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(@PathVariable Long id) {
        TicketResponse ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(ticket, "Ticket details retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ticket properties", description = "Restricted by business rules: Reporters can edit details if ticket is open; Assignees can change status; Managers/Admins can edit everything.")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TicketResponse updated = ticketService.updateTicket(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Ticket updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ticket (Reporter or Admin only)", description = "Permanently deletes a ticket by ID. Logs audit entry.")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        ticketService.deleteTicket(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Ticket deleted successfully"));
    }

    @GetMapping
    @Operation(summary = "Search and filter tickets dynamically", description = "Allows keyword searches and filtering by category, priority, status, reporter, and assignee.")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> searchTickets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long priorityId,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Page<TicketResponse> tickets = ticketService.searchTickets(
                keyword, categoryId, priorityId, statusId, reporterId, assigneeId, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(ApiResponse.success(tickets, "Tickets retrieved successfully"));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a ticket", description = "Registers comment content from the currently logged in user.")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CommentResponse comment = ticketService.addComment(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(comment, "Comment added successfully"));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file attachment to a ticket", description = "Uploads a document or image, saves it locally, and saves metadata in the database.")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to upload empty file"));
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        String cleanFileName = UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = this.fileStorageLocation.resolve(cleanFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Standardized download URL
        String fileUrl = "/api/v1/tickets/attachments/download/" + cleanFileName;

        AttachmentResponse attachment = ticketService.addAttachment(
                id, originalFileName, file.getContentType(), fileUrl, userDetails.getUsername()
        );

        return ResponseEntity.ok(ApiResponse.success(attachment, "File uploaded successfully"));
    }

    @GetMapping("/attachments/download/{fileName:.+}")
    @Operation(summary = "Download a ticket file attachment", description = "Serves the file content physically from storage.")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                String contentType = "application/octet-stream";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/activities")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get system activity logs (Admin only)", description = "Retrieve a global audit trail of all ticket updates and admin actions.")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getAllActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ActivityLogResponse> logs = ticketService.getAllActivityLogs(page, size);
        return ResponseEntity.ok(ApiResponse.success(logs, "Activity logs retrieved successfully"));
    }
}
