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
        var parentCategoryDto = Optional.ofNullable(categoryDto.getParentCategoryDto())
                .orElseGet(CategoryDto::new);

        return Category.builder()
                .categoryId(categoryDto.getCategoryId())
                .categoryTitle(categoryDto.getCategoryTitle())
                .parentCategory(
                        Category.builder()
                                .categoryId(parentCategoryDto.getCategoryId())
                                .categoryTitle(parentCategoryDto.getCategoryTitle())
                                .build())
                .build();
    }
}
