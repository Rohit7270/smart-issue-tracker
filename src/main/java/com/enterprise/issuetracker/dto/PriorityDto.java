package com.enterprise.issuetracker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriorityDto {
    private Long id;

    @NotBlank(message = "Priority name is required")
    @Size(min = 2, max = 30, message = "Priority name must be between 2 and 30 characters")
    private String name;

    @NotNull(message = "Weight is required")
    @Min(value = 1, message = "Weight must be at least 1")
    @Max(value = 10, message = "Weight must not exceed 10")
    private Integer weight;
}
