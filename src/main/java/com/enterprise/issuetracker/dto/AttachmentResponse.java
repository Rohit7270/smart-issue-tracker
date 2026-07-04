package com.enterprise.issuetracker.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentResponse {
    private Long id;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private LocalDateTime createdAt;
}
