package com.enterprise.issuetracker.mapper;

import com.enterprise.issuetracker.dto.StatusDto;
import com.enterprise.issuetracker.entity.Status;

public class StatusMapper {

    public static StatusDto toDto(Status status) {
        if (status == null) return null;
        return StatusDto.builder()
                .id(status.getId())
                .name(status.getName())
                .closedStatus(status.isClosedStatus())
                .build();
    }

    public static Status toEntity(StatusDto dto) {
        if (dto == null) return null;
        return Status.builder()
                .id(dto.getId())
                .name(dto.getName())
                .closedStatus(dto.isClosedStatus())
                .build();
    }
}
