package com.enterprise.issuetracker.service;

import com.enterprise.issuetracker.dto.PriorityDto;

import java.util.List;

public interface PriorityService {
    PriorityDto createPriority(PriorityDto priorityDto);
    List<PriorityDto> getAllPriorities();
    PriorityDto getPriorityById(Long id);
    PriorityDto updatePriority(Long id, PriorityDto priorityDto);
    void deletePriority(Long id);
}
