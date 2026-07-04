package com.enterprise.issuetracker.mapper;

import com.enterprise.issuetracker.dto.ActivityLogResponse;
import com.enterprise.issuetracker.entity.ActivityLog;

public class ActivityLogMapper {

    public static ActivityLogResponse toResponse(ActivityLog log) {
        if (log == null) return null;
        return ActivityLogResponse.builder()
                .id(log.getId())
                .ticketId(log.getTicketId())
                .user(UserMapper.toResponse(log.getUser()))
                .action(log.getAction())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
