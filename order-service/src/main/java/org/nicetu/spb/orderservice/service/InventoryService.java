package org.nicetu.spb.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.orderservice.model.dto.order.OrderDto;
import org.nicetu.spb.orderservice.model.dto.order.OrderItemDto;
import org.nicetu.spb.orderservice.model.dto.product.ProductDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Comparator;


@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final CallAPI callAPI;

    public Mono<Boolean> reserveProducts(OrderDto orderDto) {
        log.info("Attempting to reserve products for order: {}", orderDto.getOrderId());

        if (orderDto == null || orderDto.getOrderItemDtos() == null) {
            log.warn("Order or order items are null");
            return Mono.just(false);
        }

        // Шаг 1: Проверяем доступность всех товаров
        return checkProductAvailability(orderDto)
                .flatMap(allAvailable -> {
                    if (!allAvailable) {
                        log.warn("Not all products available for order: {}", orderDto.getOrderId());
                        return Mono.just(false);
                    }

                    // Шаг 2: Резервируем товары последовательно для безопасности
                    return reserveProductsSequentially(orderDto.getOrderItemDtos())
                            .doOnSuccess(success -> {
                                if (success) {
                                    log.info("Successfully reserved all products for order: {}",
                                            orderDto.getOrderId());
                                } else {
                                    log.warn("Failed to reserve some products for order: {}",
                                            orderDto.getOrderId());
                                }
                            });
                })
                .onErrorResume(error -> {
                    log.error("Error during product reservation for order {}: {}",
                            orderDto.getOrderId(), error.getMessage());
                    return Mono.just(false);
                });
    }

    private Mono<Boolean> checkProductAvailability(OrderDto orderDto) {
        log.debug("Checking product availability for order: {}", orderDto.getOrderId());

        return Flux.fromIterable(orderDto.getOrderItemDtos())
                .flatMapSequential(this::checkSingleProductAvailability, 5)
                .all(available -> available)
                .doOnSuccess(allAvailable ->
                        log.debug("All products available for order {}: {}",
                                orderDto.getOrderId(), allAvailable))
                .onErrorReturn(false);
    }

    private Mono<Boolean> checkSingleProductAvailability(OrderItemDto orderItem) {
        return callAPI.receiverProductDto(orderItem.getProductId())
                .map(productDto -> {
                    boolean available = productDto.getQuantity() >= orderItem.getQuantity();
                    if (!available) {
                        log.warn("Insufficient stock for product {}: available={}, requested={}",
                                productDto.getProductId(), productDto.getQuantity(), orderItem.getQuantity());
                    }
                    return available;
                })
                .defaultIfEmpty(false)
                .onErrorReturn(false);
    }

    private Mono<Boolean> reserveProductsSequentially(Set<OrderItemDto> orderItems) {
        // Преобразуем в список для последовательной обработки
        List<OrderItemDto> itemsList = new ArrayList<>(orderItems);

        // Сортируем по productId для предотвращения deadlock при параллельных заказах
        itemsList.sort(Comparator.comparing(OrderItemDto::getProductId));

        return reserveProductRecursive(itemsList, 0);
    }

    private Mono<Boolean> reserveProductRecursive(List<OrderItemDto> items, int index) {
        if (index >= items.size()) {
            return Mono.just(true);
        }

        OrderItemDto currentItem = items.get(index);

        return reserveSingleProduct(currentItem)
                .flatMap(success -> {
                    if (!success) {
                        log.warn("Failed to reserve product {} for order item",
                                currentItem.getProductId());
                        return Mono.just(false);
                    }
                    // Рекурсивно обрабатываем следующий продукт
                    return reserveProductRecursive(items, index + 1);
                });
    }

    private Mono<Boolean> reserveSingleProduct(OrderItemDto orderItem) {
        log.debug("Reserving {} units of product {}",
                orderItem.getQuantity(), orderItem.getProductId());

        return callAPI.reserveProduct(orderItem.getProductId(), orderItem.getQuantity())
                .map(productDto -> {
                    log.debug("Successfully reserved product {}: new quantity={}",
                            productDto.getProductId(), productDto.getQuantity());
                    return true;
                })
                .defaultIfEmpty(false)
                .onErrorResume(error -> {
                    log.error("Error reserving product {}: {}",
                            orderItem.getProductId(), error.getMessage());
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> releaseProducts(OrderDto orderDto) {
        log.info("Releasing products for order: {}", orderDto.getOrderId());

        if (orderDto == null || orderDto.getOrderItemDtos() == null) {
            return Mono.just(false);
        }

        return Flux.fromIterable(orderDto.getOrderItemDtos())
                .flatMapSequential(this::releaseSingleProduct, 5)
                .all(released -> released)
                .doOnSuccess(allReleased ->
                        log.info("All products released for order {}: {}",
                                orderDto.getOrderId(), allReleased))
                .onErrorResume(error -> {
                    log.error("Error releasing products for order {}: {}",
                            orderDto.getOrderId(), error.getMessage());
                    return Mono.just(false);
                });
    }

    private Mono<Boolean> releaseSingleProduct(OrderItemDto orderItem) {
        log.debug("Releasing {} units of product {}",
                orderItem.getQuantity(), orderItem.getProductId());

        return callAPI.releaseProduct(orderItem.getProductId(), orderItem.getQuantity())
                .map(productDto -> {
                    log.debug("Successfully released product {}: new quantity={}",
                            productDto.getProductId(), productDto.getQuantity());
                    return true;
                })
                .defaultIfEmpty(false)
                .onErrorResume(error -> {
                    log.error("Error releasing product {}: {}",
                            orderItem.getProductId(), error.getMessage());
                    return Mono.just(false);
                });
    }

    public void calculateOrderTotal(OrderDto orderDto) {
        if (orderDto == null || orderDto.getOrderItemDtos() == null) {
            orderDto.setOrderFee(0.0);
            return;
        }

        double total = 0.0;
        for (OrderItemDto item : orderDto.getOrderItemDtos()) {
            if (item != null) {
                double price = item.getPrice() != null ? item.getPrice() : 0.0;
                int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                double itemTotal = price * quantity;

                // Устанавливаем totalPrice для каждого item
                item.setTotalPrice(itemTotal);
                total += itemTotal;

                log.debug("Item calculation - Product: {}, Price: {}, Quantity: {}, Total: {}",
                        item.getProductId(), price, quantity, itemTotal);
            }
        }

        orderDto.setOrderFee(Math.round(total * 100.0) / 100.0); // Округляем до 2 знаков
        log.info("Order {} total calculated: {}",
                orderDto.getOrderId() != null ? orderDto.getOrderId() : "NEW",
                orderDto.getOrderFee());
    }

    public Mono<Boolean> updateProductQuantities(OrderDto orderDto) {
        log.info("Updating product quantities for order: {}", orderDto.getOrderId());

        return Flux.fromIterable(orderDto.getOrderItemDtos())
                .flatMapSequential(this::updateSingleProductQuantity, 5)
                .all(updated -> updated)
                .doOnSuccess(allUpdated ->
                        log.info("All product quantities updated for order {}: {}",
                                orderDto.getOrderId(), allUpdated))
                .onErrorResume(error -> {
                    log.error("Error updating product quantities for order {}: {}",
                            orderDto.getOrderId(), error.getMessage());
                    return Mono.just(false);
                });
    }

    private Mono<Boolean> updateSingleProductQuantity(OrderItemDto orderItem) {
        return callAPI.updateProductQuantity(orderItem.getProductId(), orderItem.getQuantity())
                .doOnSuccess(success ->
                        log.debug("Product {} quantity update result: {}",
                                orderItem.getProductId(), success))
                .onErrorReturn(false);
    }

    public Mono<Map<Long, ProductDto>> getProductsInfo(Set<Long> productIds) {
        log.debug("Fetching info for {} products", productIds.size());

        return Flux.fromIterable(productIds)
                .flatMap(productId ->
                        callAPI.receiverProductDto(productId)
                                .doOnError(error ->
                                        log.warn("Failed to fetch product {}: {}", productId, error.getMessage()))
                                .onErrorResume(error -> Mono.empty())
                )
                .collectMap(ProductDto::getProductId, productDto -> productDto)
                .doOnSuccess(productsMap ->
                        log.debug("Fetched info for {} unique products", productsMap.size()));
    }
}