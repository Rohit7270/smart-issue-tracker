package com.enterprise.issuetracker.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogResponse {
    private Long id;
    private Long ticketId;
    private UserResponse user;
    private String action;
    private LocalDateTime createdAt;
}
