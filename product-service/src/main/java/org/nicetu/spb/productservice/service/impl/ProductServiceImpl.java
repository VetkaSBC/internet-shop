package org.nicetu.spb.productservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.productservice.exception.wrapper.ProductNotFoundException;
import org.nicetu.spb.productservice.mapper.ProductMapping;
import org.nicetu.spb.productservice.model.ProductCategory;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.model.entity.Product;
import org.nicetu.spb.productservice.repository.CategoryRepository;
import org.nicetu.spb.productservice.repository.ProductCategoryRepository;
import org.nicetu.spb.productservice.repository.ProductRepository;
import org.nicetu.spb.productservice.service.ProductService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final TransactionalOperator transactionalOperator;
    private final ReactiveTransactionManager transactionManager;

    @Override
    public Mono<Page<ProductDto>> findAll(int page, int size) {
        log.info("Fetching products with pagination: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        return productRepository.findAllBy(pageable)
                .flatMap(this::enrichProductWithCategories)
                .map(ProductMapping::mapToDto)
                .collectList()
                .zipWith(productRepository.count())
                .map(tuple -> {
                    List<ProductDto> content = tuple.getT1();
                    Long total = tuple.getT2();
                    Page<ProductDto> pageResult = new PageImpl<>(content, pageable, total);
                    return (Page<ProductDto>) pageResult;
                })
                .onErrorResume(e -> {
                    log.error("Error fetching products: {}", e.getMessage());
                    return Mono.error(new ProductNotFoundException("Error fetching products", e));
                });
    }

    @Override
    public Mono<ProductDto> findById(Long productId) {
        log.info("Fetching product by id: {}", productId);

        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(
                        "Product not found with id: " + productId
                )))
                .flatMap(this::enrichProductWithCategories)
                .map(ProductMapping::mapToDto)
                .onErrorResume(e -> {
                    if (e instanceof ProductNotFoundException) {
                        return Mono.error(e);
                    }
                    log.error("Error fetching product {}: {}", productId, e.getMessage());
                    return Mono.error(new ProductNotFoundException("Error fetching product", e));
                });
    }

    @Override
    public Mono<ProductDto> save(ProductDto productDto) {
        log.info("Saving product: {}", productDto.getTitle());

        return transactionalOperator.transactional(
                Mono.defer(() -> {
                    Product product = ProductMapping.mapToEntity(productDto);

                    return productRepository.save(product)
                            .flatMap(savedProduct -> {
                                if (productDto.getCategories() != null && !productDto.getCategories().isEmpty()) {
                                    return saveProductCategories(savedProduct, productDto)
                                            .thenReturn(savedProduct);
                                }
                                return Mono.just(savedProduct);
                            })
                            .flatMap(this::enrichProductWithCategories)
                            .map(ProductMapping::mapToDto);
                })
        ).onErrorResume(e -> {
            log.error("Error saving product: {}", e.getMessage());
            return Mono.error(new ProductNotFoundException("Error saving product", e));
        });
    }

    @Override
    public Mono<ProductDto> update(ProductDto productDto) {
        log.info("Updating product: {}", productDto.getProductId());

        return transactionalOperator.transactional(
                Mono.defer(() -> {
                    return productRepository.findById(productDto.getProductId())
                            .switchIfEmpty(Mono.error(new ProductNotFoundException(
                                    "Product not found with id: " + productDto.getProductId()
                            )))
                            .flatMap(existingProduct -> {
                                existingProduct.setTitle(productDto.getTitle());
                                existingProduct.setDescription(productDto.getDescription());
                                existingProduct.setQuantity(productDto.getQuantity());
                                existingProduct.setPriceUnit(productDto.getPriceUnit());
                                existingProduct.setDiscount(productDto.getDiscount());
                                existingProduct.calculateQuantityStatus();

                                return productRepository.save(existingProduct);
                            })
                            .flatMap(updatedProduct -> {
                                if (productDto.getCategories() != null) {
                                    return productCategoryRepository.deleteByProductId(updatedProduct.getProductId())
                                            .then(saveProductCategories(updatedProduct, productDto))
                                            .thenReturn(updatedProduct);
                                }
                                return Mono.just(updatedProduct);
                            })
                            .flatMap(this::enrichProductWithCategories)
                            .map(ProductMapping::mapToDto);
                })
        ).retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                .filter(OptimisticLockingFailureException.class::isInstance)
        ).onErrorResume(e -> {
            if (e instanceof ProductNotFoundException) {
                return Mono.error(e);
            }
            log.error("Error updating product: {}", e.getMessage());
            return Mono.error(new ProductNotFoundException("Error updating product", e));
        });
    }

    @Override
    public Mono<ProductDto> update(Long productId, ProductDto productDto) {
        productDto.setProductId(productId);
        return update(productDto);
    }

    @Override
    public Mono<Void> deleteById(Long productId) {
        log.info("Deleting product: {}", productId);

        return transactionalOperator.transactional(
                Mono.defer(() -> {
                    return productRepository.findById(productId)
                            .switchIfEmpty(Mono.error(new ProductNotFoundException(
                                    "Product not found with id: " + productId
                            )))
                            .flatMap(product ->
                                    productCategoryRepository.deleteByProductId(productId)
                                            .then(productRepository.delete(product))
                            )
                            .then();
                })
        ).onErrorResume(e -> {
            log.error("Error deleting product {}: {}", productId, e.getMessage());
            return Mono.error(new ProductNotFoundException("Error deleting product", e));
        });
    }

    @Override
    public Mono<ProductDto> reserveProduct(Long productId, Integer quantity) {
        log.info("Reserving product: {}, quantity: {}", productId, quantity);

        return transactionalOperator.transactional(
                productRepository.reserveQuantity(productId, quantity)
                        .switchIfEmpty(Mono.error(new ProductNotFoundException(
                                "Not enough stock or product not found"
                        )))
                        .flatMap(this::enrichProductWithCategories)
                        .map(ProductMapping::mapToDto)
        ).retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                .filter(OptimisticLockingFailureException.class::isInstance)
        ).onErrorResume(e -> {
            log.error("Error reserving product {}: {}", productId, e.getMessage());
            return Mono.error(new ProductNotFoundException("Error reserving product", e));
        });
    }

    @Override
    public Mono<ProductDto> releaseProduct(Long productId, Integer quantity) {
        log.info("Releasing product: {}, quantity: {}", productId, quantity);

        return transactionalOperator.transactional(
                productRepository.incrementQuantity(productId, quantity)
                        .switchIfEmpty(Mono.error(new ProductNotFoundException(
                                "Product not found"
                        )))
                        .flatMap(this::enrichProductWithCategories)
                        .map(ProductMapping::mapToDto)
        ).retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                .filter(OptimisticLockingFailureException.class::isInstance)
        ).onErrorResume(e -> {
            log.error("Error releasing product {}: {}", productId, e.getMessage());
            return Mono.error(new ProductNotFoundException("Error releasing product", e));
        });
    }

    @Override
    public Mono<Boolean> isProductAvailable(Long productId, Integer quantity) {
        log.info("Checking availability for product: {}, quantity: {}", productId, quantity);

        return productRepository.findById(productId)
                .map(product -> product.getQuantity() >= quantity)
                .defaultIfEmpty(false);
    }

    private Mono<Void> saveProductCategories(Product product, ProductDto productDto) {
        return Flux.fromIterable(productDto.getCategories())
                .map(categoryDto -> new ProductCategory(product.getProductId(), categoryDto.getCategoryId()))
                .collectList()
                .flatMapMany(productCategoryRepository::saveAll)
                .then();
    }

    private Mono<Product> enrichProductWithCategories(Product product) {
        return categoryRepository.findByProductId(product.getProductId())
                .collectList()
                .doOnNext(categories ->
                        product.setCategories(new HashSet<>(categories))
                )
                .thenReturn(product);
    }
}