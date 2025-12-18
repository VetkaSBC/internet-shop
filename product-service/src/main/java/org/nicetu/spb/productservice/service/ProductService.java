package org.nicetu.spb.productservice.service;

import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Mono;

public interface ProductService {

    Mono<Page<ProductDto>> findAll(int page, int size);

    Mono<ProductDto> findById(final Long productId);

    Mono<ProductDto> save(final ProductDto productDto);

    Mono<ProductDto> update(final ProductDto productDto);

    Mono<ProductDto> update(final Long productId, final ProductDto productDto);

    Mono<Void> deleteById(final Long productId);

    Mono<ProductDto> releaseProduct(Long productId, Integer quantity);

    Mono<Boolean> isProductAvailable(Long productId, Integer quantity);

    Mono<ProductDto> reserveProduct(Long productId, Integer quantity);
}