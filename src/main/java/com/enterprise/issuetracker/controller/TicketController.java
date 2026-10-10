package com.enterprise.issuetracker.controller;

import com.enterprise.issuetracker.dto.*;
import com.enterprise.issuetracker.service.FileStorageService;
import com.enterprise.issuetracker.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
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

import java.util.List;

/**
 * Controller handling all ticket/issue endpoints.
 * Demonstrates clean REST conventions with constructor injection.
 */
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final FileStorageService fileStorageService;

    @Autowired
    public TicketController(TicketService ticketService, FileStorageService fileStorageService) {
        this.ticketService = ticketService;
        this.fileStorageService = fileStorageService;
    }

    // 1. Create a new ticket
    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody TicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TicketResponse created = ticketService.createTicket(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Ticket created successfully"));
    }

    // 2. Get ticket by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(@PathVariable Long id) {
        TicketResponse ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(ticket, "Ticket details retrieved successfully"));
    }

    // 3. Update ticket details
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TicketResponse updated = ticketService.updateTicket(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Ticket updated successfully"));
    }

    // 4. Assign ticket to a developer (Manager/Admin only)
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TicketResponse updated = ticketService.assignTicket(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Ticket assigned successfully"));
    }

    // 5. Update ticket status (e.g. Open -> In Progress -> Resolved)
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicketStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TicketResponse updated = ticketService.updateTicketStatus(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Ticket status updated successfully"));
    }

    // 6. Get assignment history for a ticket
    @GetMapping("/{id}/assignments")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getTicketAssignments(@PathVariable Long id) {
        List<AssignmentResponse> assignments = ticketService.getTicketAssignments(id);
        return ResponseEntity.ok(ApiResponse.success(assignments, "Assignment history retrieved successfully"));
    }

    // 7. Delete ticket (Reporter or Admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        ticketService.deleteTicket(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Ticket deleted successfully"));
    }

    // 8. Search and filter tickets
    @GetMapping
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

    // 9. Add comment to ticket
    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        CommentResponse comment = ticketService.addComment(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(comment, "Comment added successfully"));
    }

    // 10. Upload file attachment
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String storedFileName = fileStorageService.storeFile(file);
        String fileUrl = "/api/v1/tickets/attachments/download/" + storedFileName;

        AttachmentResponse attachment = ticketService.addAttachment(
                id, file.getOriginalFilename(), file.getContentType(), fileUrl, userDetails.getUsername()
        );

        return ResponseEntity.ok(ApiResponse.success(attachment, "File uploaded successfully"));
    }

    // 11. Download file attachment
    @GetMapping("/attachments/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    // 12. View system activity logs (Admin only)
    @GetMapping("/activities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getAllActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ActivityLogResponse> logs = ticketService.getAllActivityLogs(page, size);
        return ResponseEntity.ok(ApiResponse.success(logs, "Activity logs retrieved successfully"));
    }
}
