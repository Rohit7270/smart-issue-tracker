package com.enterprise.issuetracker.service;

import com.enterprise.issuetracker.dto.ActivityLogResponse;
import com.enterprise.issuetracker.dto.AttachmentResponse;
import com.enterprise.issuetracker.dto.CommentRequest;
import com.enterprise.issuetracker.dto.CommentResponse;
import com.enterprise.issuetracker.dto.TicketRequest;
import com.enterprise.issuetracker.dto.TicketResponse;
import org.springframework.data.domain.Page;

public interface TicketService {
    TicketResponse createTicket(TicketRequest request, String username);
    
    TicketResponse updateTicket(Long id, TicketRequest request, String username);
    
    void deleteTicket(Long id, String username);
    
    TicketResponse getTicketById(Long id);
    
    Page<TicketResponse> searchTickets(
            String keyword,
            Long categoryId,
            Long priorityId,
            Long statusId,
            Long reporterId,
            Long assigneeId,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    CommentResponse addComment(Long ticketId, CommentRequest request, String username);

    AttachmentResponse addAttachment(Long ticketId, String fileName, String fileType, String fileUrl, String username);

    Page<ActivityLogResponse> getAllActivityLogs(int page, int size);
}
