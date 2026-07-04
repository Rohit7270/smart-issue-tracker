package com.enterprise.issuetracker.service.impl;

import com.enterprise.issuetracker.dto.PriorityDto;
import com.enterprise.issuetracker.entity.Priority;
import com.enterprise.issuetracker.exception.BadRequestException;
import com.enterprise.issuetracker.exception.ResourceNotFoundException;
import com.enterprise.issuetracker.mapper.PriorityMapper;
import com.enterprise.issuetracker.repository.PriorityRepository;
import com.enterprise.issuetracker.service.PriorityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PriorityServiceImpl implements PriorityService {

    private final PriorityRepository priorityRepository;

    @Autowired
    public PriorityServiceImpl(PriorityRepository priorityRepository) {
        this.priorityRepository = priorityRepository;
    }

    @Override
    @Transactional
    public PriorityDto createPriority(PriorityDto priorityDto) {
        log.info("Creating new priority: {}", priorityDto.getName());
        if (priorityRepository.existsByName(priorityDto.getName())) {
            throw new BadRequestException("Priority name '" + priorityDto.getName() + "' already exists!");
        }
        Priority priority = PriorityMapper.toEntity(priorityDto);
        Priority saved = priorityRepository.save(priority);
        return PriorityMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriorityDto> getAllPriorities() {
        log.info("Fetching all priorities");
        return priorityRepository.findAll().stream()
                .map(PriorityMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PriorityDto getPriorityById(Long id) {
        log.info("Fetching priority by ID: {}", id);
        Priority priority = priorityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Priority not found with ID: " + id));
        return PriorityMapper.toDto(priority);
    }

    @Override
    @Transactional
    public PriorityDto updatePriority(Long id, PriorityDto priorityDto) {
        log.info("Updating priority with ID: {}", id);
        Priority priority = priorityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Priority not found with ID: " + id));

        priorityRepository.findByName(priorityDto.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("Priority name '" + priorityDto.getName() + "' already exists on another record!");
            }
        });

        priority.setName(priorityDto.getName());
        priority.setWeight(priorityDto.getWeight());
        Priority updated = priorityRepository.save(priority);
        return PriorityMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deletePriority(Long id) {
        log.info("Deleting priority with ID: {}", id);
        Priority priority = priorityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Priority not found with ID: " + id));
        priorityRepository.delete(priority);
    }
}
