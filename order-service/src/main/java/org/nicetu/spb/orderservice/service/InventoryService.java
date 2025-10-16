package org.nicetu.spb.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.orderservice.exception.wrapper.ProductNotFoundException;
import org.nicetu.spb.orderservice.model.dto.order.OrderDto;
import org.nicetu.spb.orderservice.model.dto.order.OrderItemDto;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final CallAPI callAPI;
    private final WebClient.Builder webClientBuilder;

    public Mono<Boolean> reserveProducts(OrderDto orderDto) {
        log.info("Reserving products for order {}", orderDto.getOrderId());

        return checkProductAvailability(orderDto)
                .flatMap(available -> {
                    if (!available) {
                        return Mono.error(new ProductNotFoundException("Not enough stock for some products"));
                    }
                    return reserveAllProducts(orderDto);
                });
    }

    private Mono<Boolean> checkProductAvailability(OrderDto orderDto) {
        return Flux.fromIterable(orderDto.getOrderItemDtos())
                .flatMap(orderItem ->
                        callAPI.receiverProductDto(orderItem.getProductId())
                                .map(productDto -> {
                                    if (productDto.getQuantity() < orderItem.getQuantity()) {
                                        log.warn("Not enough stock for product {}. Available: {}, Requested: {}",
                                                productDto.getProductId(), productDto.getQuantity(), orderItem.getQuantity());
                                        return false;
                                    }
                                    return true;
                                })
                                .onErrorReturn(false)
                )
                .all(result -> result)
                .onErrorReturn(false);
    }

    private Mono<Boolean> reserveAllProducts(OrderDto orderDto) {
        return Flux.fromIterable(orderDto.getOrderItemDtos())
                .flatMap(orderItem ->
                        callAPI.receiverProductDto(orderItem.getProductId())
                                .flatMap(productDto -> {
                                    int newQuantity = productDto.getQuantity() - orderItem.getQuantity();
                                    return updateProductQuantity(productDto.getProductId(), newQuantity)
                                            .doOnSuccess(result ->
                                                    log.info("Reserved {} units of product {}. New quantity: {}",
                                                            orderItem.getQuantity(), productDto.getProductId(), newQuantity)
                                            );
                                })
                )
                .then(Mono.just(true))
                .onErrorResume(e -> {
                    log.error("Error reserving products: {}", e.getMessage());
                    return rollbackProductReservation(orderDto)
                            .then(Mono.error(e));
                });
    }

    public Mono<Boolean> releaseProducts(OrderDto orderDto) {
        log.info("Releasing products for order {}", orderDto.getOrderId());

        return Flux.fromIterable(orderDto.getOrderItemDtos())
                .flatMap(orderItem ->
                        callAPI.receiverProductDto(orderItem.getProductId())
                                .flatMap(productDto -> {
                                    int newQuantity = productDto.getQuantity() + orderItem.getQuantity();
                                    return updateProductQuantity(productDto.getProductId(), newQuantity)
                                            .doOnSuccess(result ->
                                                    log.info("Released {} units of product {}. New quantity: {}",
                                                            orderItem.getQuantity(), productDto.getProductId(), newQuantity)
                                            );
                                })
                )
                .then(Mono.just(true));
    }

    private Mono<Boolean> rollbackProductReservation(OrderDto orderDto) {
        log.info("Rolling back product reservation for order {}", orderDto.getOrderId());
        return releaseProducts(orderDto);
    }

    private Mono<Boolean> updateProductQuantity(Long productId, Integer newQuantity) {
        return webClientBuilder.build()
                .patch()
                .uri("http://localhost:8089/api/products/inventory/{productId}/quantity?quantity={quantity}",
                        productId, newQuantity)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error updating product quantity for product {}: {}", productId, e.getMessage());
                    return Mono.error(new ProductNotFoundException("Failed to update product quantity for product " + productId));
                });
    }

    public void calculateOrderTotal(OrderDto orderDto) {
        if (orderDto == null || orderDto.getOrderItemDtos() == null || orderDto.getOrderItemDtos().isEmpty()) {
            orderDto.setOrderFee(0.0);
            log.warn("Order or order items are null/empty, setting total to 0.0");
            return;
        }

        double total = orderDto.getOrderItemDtos().stream()
                .filter(Objects::nonNull)
                .mapToDouble(orderItem -> {
                    Double price = orderItem.getPrice();
                    Integer quantity = orderItem.getQuantity();

                    if (price == null) {
                        log.warn("Price is null for productId: {}, setting to 0.0", orderItem.getProductId());
                        price = 0.0;
                    }
                    if (quantity == null) {
                        log.warn("Quantity is null for productId: {}, setting to 0", orderItem.getProductId());
                        quantity = 0;
                    }

                    double itemTotal = price * quantity;
                    orderItem.setTotalPrice(itemTotal);
                    return itemTotal;
                })
                .sum();

        orderDto.setOrderFee(total);
        log.info("Calculated order total: {}", total);
    }
}