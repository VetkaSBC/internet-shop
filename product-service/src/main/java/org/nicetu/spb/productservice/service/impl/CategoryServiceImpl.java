package org.nicetu.spb.productservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.productservice.exception.wrapper.CategoryNotFoundException;
import org.nicetu.spb.productservice.mapper.CategoryMapping;
import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.nicetu.spb.productservice.model.entity.Category;
import org.nicetu.spb.productservice.repository.CategoryRepository;
import org.nicetu.spb.productservice.repository.ProductCategoryRepository;
import org.nicetu.spb.productservice.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<Page<CategoryDto>> findAllCategory(int page, int size) {
        log.info("Fetching categories with pagination: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        return categoryRepository.findAllBy(pageable)
                .flatMap(this::enrichCategoryWithRelations)
                .map(CategoryMapping::mapToDto)
                .collectList()
                .zipWith(categoryRepository.count())
                .map(tuple -> {
                    List<CategoryDto> content = tuple.getT1();
                    Long total = tuple.getT2();
                    Page<CategoryDto> pageResult = new PageImpl<>(content, pageable, total);
                    return pageResult;
                })
                .onErrorResume(e -> {
                    log.error("Error fetching categories: {}", e.getMessage());
                    return Mono.error(new CategoryNotFoundException("Error fetching categories", e));
                });
    }

    @Override
    public Mono<CategoryDto> findById(Integer categoryId) {
        log.info("Fetching category by id: {}", categoryId);

        return categoryRepository.findById(categoryId)
                .switchIfEmpty(Mono.error(new CategoryNotFoundException(
                        "Category not found with id: " + categoryId
                )))
                .flatMap(this::enrichCategoryWithRelations)
                .map(CategoryMapping::mapToDto)
                .onErrorResume(e -> {
                    if (e instanceof CategoryNotFoundException) {
                        return Mono.error(e);
                    }
                    log.error("Error fetching category {}: {}", categoryId, e.getMessage());
                    return Mono.error(new CategoryNotFoundException("Error fetching category", e));
                });
    }

    @Override
    public Mono<CategoryDto> save(CategoryDto categoryDto) {
        log.info("Saving category: {}", categoryDto.getCategoryTitle());

        return transactionalOperator.transactional(
                Mono.defer(() -> {
                    Category category = CategoryMapping.mapToEntity(categoryDto);
                    return categoryRepository.save(category)
                            .flatMap(this::enrichCategoryWithRelations)
                            .map(CategoryMapping::mapToDto);
                })
        ).onErrorResume(e -> {
            log.error("Error saving category: {}", e.getMessage());
            return Mono.error(new CategoryNotFoundException("Error saving category", e));
        });
    }

    @Override
    public Mono<CategoryDto> update(CategoryDto categoryDto) {
        log.info("Updating category: {}", categoryDto.getCategoryId());

        return transactionalOperator.transactional(
                Mono.defer(() -> {
                    return categoryRepository.findById(categoryDto.getCategoryId())
                            .switchIfEmpty(Mono.error(new CategoryNotFoundException(
                                    "Category not found with id: " + categoryDto.getCategoryId()
                            )))
                            .flatMap(existingCategory -> {
                                existingCategory.setCategoryTitle(categoryDto.getCategoryTitle());
                                existingCategory.setParentCategoryId(
                                        categoryDto.getParentCategoryDto() != null ?
                                                categoryDto.getParentCategoryDto().getCategoryId() : null
                                );
                                return categoryRepository.save(existingCategory);
                            })
                            .flatMap(this::enrichCategoryWithRelations)
                            .map(CategoryMapping::mapToDto);
                })
        ).onErrorResume(e -> {
            if (e instanceof CategoryNotFoundException) {
                return Mono.error(e);
            }
            log.error("Error updating category: {}", e.getMessage());
            return Mono.error(new CategoryNotFoundException("Error updating category", e));
        });
    }

    @Override
    public Mono<CategoryDto> update(Integer categoryId, CategoryDto categoryDto) {
        categoryDto.setCategoryId(categoryId);
        return update(categoryDto);
    }

    @Override
    public Mono<Void> deleteById(Integer categoryId) {
        log.info("Deleting category: {}", categoryId);

        return transactionalOperator.transactional(
                Mono.defer(() -> {
                    return categoryRepository.findById(categoryId)
                            .switchIfEmpty(Mono.error(new CategoryNotFoundException(
                                    "Category not found with id: " + categoryId
                            )))
                            .flatMap(category ->
                                    categoryRepository.hasSubcategories(categoryId)
                                            .flatMap(hasSubcategories -> {
                                                if (hasSubcategories) {
                                                    return Mono.error(new CategoryNotFoundException(
                                                            "Cannot delete category with subcategories"
                                                    ));
                                                }
                                                return productCategoryRepository.deleteByCategoryId(categoryId)
                                                        .then(categoryRepository.delete(category));
                                            })
                            )
                            .then();
                })
        ).onErrorResume(e -> {
            log.error("Error deleting category {}: {}", categoryId, e.getMessage());
            return Mono.error(new CategoryNotFoundException("Error deleting category", e));
        });
    }

    private Mono<Category> enrichCategoryWithRelations(Category category) {
        return Mono.zip(
                categoryRepository.findByParentCategoryId(category.getCategoryId()).collectList(),
                productCategoryRepository.findProductIdsByCategoryId(category.getCategoryId()).collectList()
        ).doOnNext(tuple -> {
            category.setSubCategories(new HashSet<>(tuple.getT1()));
        }).thenReturn(category);
    }
}