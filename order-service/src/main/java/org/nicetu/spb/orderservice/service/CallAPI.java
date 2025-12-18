package org.nicetu.spb.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.orderservice.model.dto.product.ProductDto;
import org.nicetu.spb.orderservice.model.dto.user.UserDto;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallAPI {

    private final WebClient userWebClient;
    private final WebClient productWebClient;

    public Mono<UserDto> receiverUserDto(Long userId, String token) {
        log.debug("Fetching user with ID: {}", userId);

        return userWebClient
                .get()
                .uri("/api/manager/user/{userId}", userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            log.error("Error fetching user {}: {}", userId, response.statusCode());
                            return Mono.error(new RuntimeException(
                                    "Failed to fetch user: " + response.statusCode()));
                        })
                .bodyToMono(UserDto.class)
                .doOnSuccess(user -> log.debug("Successfully fetched user: {}", user.getId()))
                .doOnError(error -> log.error("Error fetching user {}: {}", userId, error.getMessage()));
    }

    public Mono<ProductDto> receiverProductDto(Long productId) {
        log.debug("Fetching product with ID: {}", productId);

        return productWebClient
                .get()
                .uri("/api/products/{productId}", productId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            log.error("Error fetching product {}: {}", productId, response.statusCode());
                            return Mono.error(new RuntimeException(
                                    "Failed to fetch product: " + response.statusCode()));
                        })
                .bodyToMono(ProductDto.class)
                .doOnSuccess(product -> log.debug("Successfully fetched product: {}", product.getProductId()))
                .doOnError(error -> log.error("Error fetching product {}: {}", productId, error.getMessage()));
    }

    public Mono<Boolean> updateProductQuantity(Long productId, Integer quantity) {
        log.debug("Updating quantity for product {} to {}", productId, quantity);

        return productWebClient
                .patch()
                .uri("/api/products/inventory/{productId}/quantity?quantity={quantity}",
                        productId, quantity)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            log.error("Error updating product {} quantity: {}", productId, response.statusCode());
                            return Mono.error(new RuntimeException(
                                    "Failed to update product quantity: " + response.statusCode()));
                        })
                .bodyToMono(Boolean.class)
                .doOnSuccess(result -> log.debug("Successfully updated product {} quantity: {}", productId, result))
                .doOnError(error -> log.error("Error updating product {} quantity: {}", productId, error.getMessage()))
                .onErrorReturn(false);
    }

    public Mono<ProductDto> reserveProduct(Long productId, Integer quantity) {
        log.debug("Reserving {} units of product {}", quantity, productId);

        return productWebClient
                .post()
                .uri("/api/products/inventory/{productId}/reserve?quantity={quantity}",
                        productId, quantity)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            log.error("Error reserving product {}: {}", productId, response.statusCode());
                            return Mono.error(new RuntimeException(
                                    "Failed to reserve product: " + response.statusCode()));
                        })
                .bodyToMono(ProductDto.class)
                .doOnSuccess(product -> log.debug("Successfully reserved product {}: {}", productId, product))
                .doOnError(error -> log.error("Error reserving product {}: {}", productId, error.getMessage()));
    }

    public Mono<ProductDto> releaseProduct(Long productId, Integer quantity) {
        log.debug("Releasing {} units of product {}", quantity, productId);

        return productWebClient
                .post()
                .uri("/api/products/inventory/{productId}/release?quantity={quantity}",
                        productId, quantity)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            log.error("Error releasing product {}: {}", productId, response.statusCode());
                            return Mono.error(new RuntimeException(
                                    "Failed to release product: " + response.statusCode()));
                        })
                .bodyToMono(ProductDto.class)
                .doOnSuccess(product -> log.debug("Successfully released product {}: {}", productId, product))
                .doOnError(error -> log.error("Error releasing product {}: {}", productId, error.getMessage()));
    }

    public Mono<Boolean> reserveProductWithOptimisticLock(Long productId, Integer quantity, Integer version) {
        log.debug("Reserving {} units of product {} with version {}", quantity, productId, version);

        return productWebClient
                .post()
                .uri("/api/products/inventory/{productId}/reserve-atomic", productId)
                .bodyValue(Map.of(
                        "quantity", quantity,
                        "version", version
                ))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            log.error("Error atomic reserving product {}: {}", productId, response.statusCode());
                            return Mono.error(new RuntimeException(
                                    "Failed to atomic reserve product: " + response.statusCode()));
                        })
                .bodyToMono(Boolean.class)
                .doOnSuccess(result -> log.debug("Atomic reserve result for product {}: {}", productId, result))
                .doOnError(error -> log.error("Error atomic reserving product {}: {}", productId, error.getMessage()));
    }
}