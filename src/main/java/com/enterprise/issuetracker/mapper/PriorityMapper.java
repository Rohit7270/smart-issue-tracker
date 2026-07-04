package com.enterprise.issuetracker.mapper;

import com.enterprise.issuetracker.dto.PriorityDto;
import com.enterprise.issuetracker.entity.Priority;

public class PriorityMapper {

    public static PriorityDto toDto(Priority priority) {
        if (priority == null) return null;
        return PriorityDto.builder()
                .id(priority.getId())
                .name(priority.getName())
                .weight(priority.getWeight())
                .build();
    }

    public static Priority toEntity(PriorityDto dto) {
        if (dto == null) return null;
        return Priority.builder()
                .id(dto.getId())
                .name(dto.getName())
                .weight(dto.getWeight())
                .build();
    }
}
