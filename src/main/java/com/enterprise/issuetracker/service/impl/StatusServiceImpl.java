package com.enterprise.issuetracker.service.impl;

import com.enterprise.issuetracker.dto.StatusDto;
import com.enterprise.issuetracker.entity.Status;
import com.enterprise.issuetracker.exception.BadRequestException;
import com.enterprise.issuetracker.exception.ResourceNotFoundException;
import com.enterprise.issuetracker.mapper.StatusMapper;
import com.enterprise.issuetracker.repository.StatusRepository;
import com.enterprise.issuetracker.service.StatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StatusServiceImpl implements StatusService {

    private final StatusRepository statusRepository;

    @Autowired
    public StatusServiceImpl(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public List<StatusDto> getAllStatuses() {
        log.info("Fetching all statuses");
        return statusRepository.findAll().stream()
                .map(StatusMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StatusDto getStatusById(Long id) {
        log.info("Fetching status by ID: {}", id);
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found with ID: " + id));
        return StatusMapper.toDto(status);
    }

    @Override
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

    @Override
    @Transactional
    public void deleteStatus(Long id) {
        log.info("Deleting status with ID: {}", id);
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found with ID: " + id));
        statusRepository.delete(status);
    }
}
