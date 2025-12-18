package org.nicetu.spb.productservice.service;

import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Mono;

public interface CategoryService {

    Mono<Page<CategoryDto>> findAllCategory(int page, int size);

    Mono<CategoryDto> findById(final Integer categoryId);

    Mono<CategoryDto> save(final CategoryDto categoryDto);

    Mono<CategoryDto> update(final CategoryDto categoryDto);

    Mono<CategoryDto> update(final Integer categoryId, final CategoryDto categoryDto);

    Mono<Void> deleteById(final Integer categoryId);
}