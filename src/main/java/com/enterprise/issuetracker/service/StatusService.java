package com.enterprise.issuetracker.service;

import com.enterprise.issuetracker.dto.StatusDto;

import java.util.List;

public interface StatusService {
    StatusDto createStatus(StatusDto statusDto);
    List<StatusDto> getAllStatuses();
    StatusDto getStatusById(Long id);
    StatusDto updateStatus(Long id, StatusDto statusDto);
    void deleteStatus(Long id);
}
