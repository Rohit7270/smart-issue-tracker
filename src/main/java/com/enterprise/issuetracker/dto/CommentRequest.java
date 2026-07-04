package com.enterprise.issuetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequest {

    @NotBlank(message = "Comment content cannot be blank")
    @Size(max = 1500, message = "Comment content must not exceed 1500 characters")
    private String content;
}
