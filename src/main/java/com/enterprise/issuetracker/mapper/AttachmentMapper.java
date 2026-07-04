package com.enterprise.issuetracker.mapper;

import com.enterprise.issuetracker.dto.AttachmentResponse;
import com.enterprise.issuetracker.entity.Attachment;

public class AttachmentMapper {

    public static AttachmentResponse toResponse(Attachment attachment) {
        if (attachment == null) return null;
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .fileType(attachment.getFileType())
                .fileUrl(attachment.getFileUrl())
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}
