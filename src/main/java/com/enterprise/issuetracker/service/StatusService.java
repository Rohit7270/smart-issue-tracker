package com.enterprise.issuetracker.service;

import com.enterprise.issuetracker.dto.StatusDto;
import com.enterprise.issuetracker.entity.Status;
import com.enterprise.issuetracker.exception.BadRequestException;
import com.enterprise.issuetracker.exception.ResourceNotFoundException;
import com.enterprise.issuetracker.mapper.StatusMapper;
import com.enterprise.issuetracker.repository.StatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StatusService {

    private final StatusRepository statusRepository;

    @Autowired
    public StatusService(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    @Transactional
    public StatusDto createStatus(StatusDto statusDto) {
        log.info("Creating new status: {}", statusDto.getName());
        if (statusRepository.existsByName(statusDto.getName())) {
            throw new BadRequestException("Status name '" + statusDto.getName() + "' already exists!");
        }
        Status status = StatusMapper.toEntity(statusDto);
        Status saved = statusRepository.save(status);
        return StatusMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<StatusDto> getAllStatuses() {
        log.info("Fetching all statuses");
        return statusRepository.findAll().stream()
                .map(StatusMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StatusDto getStatusById(Long id) {
        log.info("Fetching status by ID: {}", id);
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found with ID: " + id));
        return StatusMapper.toDto(status);
    }

    @Transactional
    public StatusDto updateStatus(Long id, StatusDto statusDto) {
        log.info("Updating status with ID: {}", id);
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found with ID: " + id));

        statusRepository.findByName(statusDto.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("Status name '" + statusDto.getName() + "' already exists on another record!");
            }
        });

        status.setName(statusDto.getName());
        status.setClosedStatus(statusDto.isClosedStatus());
        Status updated = statusRepository.save(status);
        return StatusMapper.toDto(updated);
    }

    @Transactional
    public void deleteStatus(Long id) {
        log.info("Deleting status with ID: {}", id);
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found with ID: " + id));
        statusRepository.delete(status);
    }
}
