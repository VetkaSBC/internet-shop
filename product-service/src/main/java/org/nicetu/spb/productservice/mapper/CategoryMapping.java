package org.nicetu.spb.productservice.mapper;

import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.nicetu.spb.productservice.model.entity.Category;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CategoryMapping {

    public static CategoryDto mapToDto(Category category) {
        if (category == null) return null;

        return CategoryDto.builder()
                .categoryId(category.getCategoryId())
                .categoryTitle(category.getCategoryTitle())
                .parentCategoryDto(category.getParentCategoryId() != null ?
                        CategoryDto.builder()
                                .categoryId(category.getParentCategoryId())
                                .build() : null)
                .subCategoriesDtos(category.getSubCategories() != null ?
                        category.getSubCategories().stream()
                                .map(CategoryMapping::mapToDto)
                                .collect(Collectors.toSet()) : null)
                .productDtos(category.getProducts() != null ?
                        category.getProducts().stream()
                                .map(ProductMapping::mapToDto)
                                .collect(Collectors.toSet()) : null)
                .build();
    }

    public static Category mapToEntity(CategoryDto categoryDto) {
        if (categoryDto == null) return null;

        return Category.builder()
                .categoryId(categoryDto.getCategoryId())
                .categoryTitle(categoryDto.getCategoryTitle())
                .parentCategoryId(categoryDto.getParentCategoryDto() != null ?
                        categoryDto.getParentCategoryDto().getCategoryId() : null)
                .build();
    }
}