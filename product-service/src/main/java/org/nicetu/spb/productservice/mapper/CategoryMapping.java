package org.nicetu.spb.productservice.mapper;

import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.nicetu.spb.productservice.model.entity.Category;

import java.util.Optional;

public interface CategoryMapping {

    static CategoryDto mapToDto(Category category) {
        var parentCategory = Optional.ofNullable(category.getParentCategory())
                .orElseGet(Category::new);

        return CategoryDto.builder()
                .categoryId(category.getCategoryId())
                .categoryTitle(category.getCategoryTitle())
                .parentCategoryDto(
                        CategoryDto.builder()
                                .categoryId(parentCategory.getCategoryId())
                                .categoryTitle(parentCategory.getCategoryTitle())
                                .build())
                .build();
    }

    static Category mapToEntity(CategoryDto categoryDto) {
        Category category = Category.builder()
                .categoryId(categoryDto.getCategoryId())
                .categoryTitle(categoryDto.getCategoryTitle())
                .build();

        if (categoryDto.getParentCategoryDto() != null &&
                categoryDto.getParentCategoryDto().getCategoryId() != null) {
            Category parentCategory = Category.builder()
                    .categoryId(categoryDto.getParentCategoryDto().getCategoryId())
                    .build();
            category.setParentCategory(parentCategory);
        }

        return category;
    }
}