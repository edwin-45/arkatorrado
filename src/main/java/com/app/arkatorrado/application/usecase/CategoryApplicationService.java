package com.app.arkatorrado.application.usecase;

import com.app.arkatorrado.domain.model.Category;
import com.app.arkatorrado.domain.port.in.CategoryUseCase;
import com.app.arkatorrado.domain.port.out.CategoryRepositoryPort;

import java.util.List;

public class CategoryApplicationService implements CategoryUseCase {

    private final CategoryRepositoryPort categoryRepository;

    public CategoryApplicationService(CategoryRepositoryPort categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    @Override
    public Category createCategory(Category category) {
        validateCategory(category);
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Long id, Category category) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found with id: " + id);
        }
        validateCategory(category);
        category.setId(id);
        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    /**
     * Domain validation logic for Category
     */
    private void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }

        if (category.getNombre() == null || category.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty");
        }

        if (category.getNombre().length() > 100) {
            throw new IllegalArgumentException("Category name cannot exceed 100 characters");
        }
    }
}
