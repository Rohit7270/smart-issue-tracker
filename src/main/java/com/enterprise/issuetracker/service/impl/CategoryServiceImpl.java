package com.enterprise.issuetracker.service.impl;

import com.enterprise.issuetracker.dto.CategoryDto;
import com.enterprise.issuetracker.entity.Category;
import com.enterprise.issuetracker.exception.BadRequestException;
import com.enterprise.issuetracker.exception.ResourceNotFoundException;
import com.enterprise.issuetracker.mapper.CategoryMapper;
import com.enterprise.issuetracker.repository.CategoryRepository;
import com.enterprise.issuetracker.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        log.info("Creating new category: {}", categoryDto.getName());
        if (categoryRepository.existsByName(categoryDto.getName())) {
            throw new BadRequestException("Category name '" + categoryDto.getName() + "' already exists!");
        }
        Category category = CategoryMapper.toEntity(categoryDto);
        Category saved = categoryRepository.save(category);
        return CategoryMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        log.info("Fetching all categories");
        return categoryRepository.findAll().stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        log.info("Fetching category by ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return CategoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        log.info("Updating category with ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        // If updating to a name that already exists in another category record
        categoryRepository.findByName(categoryDto.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("Category name '" + categoryDto.getName() + "' already exists on another category record!");
            }
        });

        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        Category updated = categoryRepository.save(category);
        return CategoryMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting category with ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        categoryRepository.delete(category);
    }
}
