package com.enterprise.issuetracker.mapper;

import com.enterprise.issuetracker.dto.CommentResponse;
import com.enterprise.issuetracker.entity.Comment;

public class CommentMapper {

    public static CommentResponse toResponse(Comment comment) {
        if (comment == null) return null;
        return CommentResponse.builder()
                .id(comment.getId())
                .ticketId(comment.getTicket().getId())
                .content(comment.getContent())
                .user(UserMapper.toResponse(comment.getUser()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
