package org.nicetu.spb.orderservice.service;

import org.nicetu.spb.orderservice.model.dto.product.ProductDto;
import org.nicetu.spb.orderservice.model.dto.user.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CallAPI {
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public CallAPI(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<UserDto> receiverUserDto(Long userId, String token) {
        return webClientBuilder.baseUrl("http://localhost:8088").build()
                .get()
                .uri("/api/manager/user/" + userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(UserDto.class);
    }

    public Mono<ProductDto> receiverProductDto(Long productId) {
        return webClientBuilder.baseUrl("http://localhost:8089").build()
                .get()
                .uri("/api/products/" + productId)
                .retrieve()
                .bodyToMono(ProductDto.class);
    }

    public Mono<Boolean> updateProductQuantity(Long productId, Integer quantity) {
        return webClientBuilder.baseUrl("http://localhost:8089").build()
                .patch()
                .uri("/api/products/inventory/{productId}/quantity?quantity={quantity}",
                        productId, quantity)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorReturn(false);
    }

    public Mono<ProductDto> reserveProduct(Long productId, Integer quantity) {
        return webClientBuilder.baseUrl("http://localhost:8089").build()
                .post()
                .uri("/api/products/inventory/{productId}/reserve?quantity={quantity}",
                        productId, quantity)
                .retrieve()
                .bodyToMono(ProductDto.class);
    }

    public Mono<ProductDto> releaseProduct(Long productId, Integer quantity) {
        return webClientBuilder.baseUrl("http://localhost:8089").build()
                .post()
                .uri("/api/products/inventory/{productId}/release?quantity={quantity}",
                        productId, quantity)
                .retrieve()
                .bodyToMono(ProductDto.class);
    }
}