package org.nicetu.spb.orderservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.orderservice.exception.wrapper.OrderNotFoundException;
import org.nicetu.spb.orderservice.mapper.OrderMappingHelper;
import org.nicetu.spb.orderservice.model.dto.order.OrderDto;
import org.nicetu.spb.orderservice.model.dto.order.OrderItemDto;
import org.nicetu.spb.orderservice.model.entity.Order;
import org.nicetu.spb.orderservice.model.entity.OrderItem;
import org.nicetu.spb.orderservice.repository.OrderRepository;
import org.nicetu.spb.orderservice.repository.OrderItemRepository;
import org.nicetu.spb.orderservice.repository.CartRepository;
import org.nicetu.spb.orderservice.security.JwtProvider;
import org.nicetu.spb.orderservice.service.CallAPI;
import org.nicetu.spb.orderservice.service.EmailService;
import org.nicetu.spb.orderservice.service.InventoryService;
import org.nicetu.spb.orderservice.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CallAPI callAPI;
    private final InventoryService inventoryService;
    private final EmailService emailService;
    private final JwtProvider jwtProvider;

    @Override
    public Mono<Page<OrderDto>> findAll(int page, int size, String sortBy, String sortOrder) {
        log.info("Fetching all orders with pagination - page: {}, size: {}, sort: {} {}",
                page, size, sortBy, sortOrder);

        Sort.Direction direction = Sort.Direction.fromString(sortOrder);
        String sortDirection = direction.isAscending() ? "ASC" : "DESC";

        return orderRepository.countAll()
                .flatMap(total -> {
                    if (total == 0) {
                        return Mono.just(Page.<OrderDto>empty());
                    }

                    return orderRepository.findAllWithPagination(sortBy, sortDirection, size, page * size)
                            .flatMapSequential(this::fetchFullOrder, 10)
                            .collectList()
                            .map(content -> {
                                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
                                return new PageImpl<>(content, pageable, total);
                            });
                })
                .doOnSuccess(pageResult ->
                        log.info("Found {} orders on page {} of {}",
                                pageResult.getContent().size(), page, pageResult.getTotalPages()))
                .doOnError(error ->
                        log.error("Error fetching orders: {}", error.getMessage()));
    }

    private Mono<OrderDto> fetchFullOrder(Order order) {
        return Mono.zip(
                orderItemRepository.findAllByOrderId(order.getOrderId()).collectList(),
                cartRepository.findById(order.getCartId()).defaultIfEmpty(null)
        ).flatMap(tuple -> {
            List<OrderItem> orderItems = tuple.getT1();

            return OrderMappingHelper.mapToDto(
                    order,
                    Flux.fromIterable(orderItems),
                    Mono.justOrEmpty(tuple.getT2())
            ).flatMap(this::enrichOrderItemsWithProductInfo);
        });
    }

    private Mono<OrderDto> enrichOrderItemsWithProductInfo(OrderDto orderDto) {
        if (orderDto.getOrderItemDtos() == null || orderDto.getOrderItemDtos().isEmpty()) {
            return Mono.just(orderDto);
        }

        // Собираем все productId для batch запроса
        Set<Long> productIds = orderDto.getOrderItemDtos().stream()
                .map(OrderItemDto::getProductId)
                .collect(Collectors.toSet());

        // Получаем все продукты параллельно
        return Flux.fromIterable(productIds)
                .flatMap(productId ->
                        callAPI.receiverProductDto(productId)
                                .doOnError(error ->
                                        log.warn("Failed to fetch product {}: {}", productId, error.getMessage()))
                                .onErrorResume(error -> Mono.empty())
                )
                .collectMap(productDto -> productDto.getProductId(), productDto -> productDto)
                .map(productMap -> {
                    // Обогащаем order items информацией о продуктах
                    orderDto.getOrderItemDtos().forEach(orderItem ->
                            orderItem.setProductDto(productMap.get(orderItem.getProductId()))
                    );
                    return orderDto;
                });
    }

    @Override
    public Mono<OrderDto> findById(Integer orderId) {
        log.info("Fetching order by ID: {}", orderId);

        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.defer(() ->
                        Mono.error(new OrderNotFoundException(
                                String.format("Order with id: %d not found", orderId)))
                ))
                .flatMap(this::fetchFullOrder)
                .doOnSuccess(order ->
                        log.info("Successfully fetched order: {}", orderId))
                .doOnError(error -> {
                    if (!(error instanceof OrderNotFoundException)) {
                        log.error("Error fetching order {}: {}", orderId, error.getMessage());
                    }
                });
    }

    @Override
    @Transactional
    public Mono<OrderDto> save(final OrderDto orderDto, final String jwtToken) {
        log.info("Saving new order");

        return validateOrder(orderDto)
                .then(Mono.defer(() -> {
                    // Рассчитываем общую сумму
                    inventoryService.calculateOrderTotal(orderDto);
                    orderDto.setStatus("NEW");

                    // Резервируем товары на складе
                    return inventoryService.reserveProducts(orderDto)
                            .flatMap(reservationSuccess -> {
                                if (!reservationSuccess) {
                                    return Mono.error(new RuntimeException(
                                            "Failed to reserve products. Inventory might be insufficient."));
                                }

                                // Сохраняем заказ
                                Order order = OrderMappingHelper.mapToEntity(orderDto);
                                return orderRepository.save(order)
                                        .flatMap(savedOrder -> {
                                            log.info("Order saved with ID: {}", savedOrder.getOrderId());

                                            // Сохраняем items заказа
                                            return saveOrderItems(orderDto, savedOrder.getOrderId())
                                                    .then(Mono.defer(() ->
                                                            findById(savedOrder.getOrderId())
                                                    ))
                                                    .doOnSuccess(savedOrderDto -> {
                                                        // Асинхронно отправляем email
                                                        sendOrderConfirmationEmail(savedOrderDto, jwtToken);
                                                    });
                                        });
                            });
                }))
                .doOnError(error ->
                        log.error("Error saving order: {}", error.getMessage()));
    }

    private Mono<Void> validateOrder(OrderDto orderDto) {
        return Mono.fromRunnable(() -> {
            if (orderDto == null) {
                throw new IllegalArgumentException("OrderDto cannot be null");
            }

            if (orderDto.getOrderItemDtos() == null || orderDto.getOrderItemDtos().isEmpty()) {
                throw new IllegalArgumentException("Order must have at least one item");
            }

            // Проверяем все items
            orderDto.getOrderItemDtos().forEach(item -> {
                if (item == null) {
                    throw new IllegalArgumentException("Order item cannot be null");
                }
                if (item.getProductId() == null) {
                    throw new IllegalArgumentException("Product ID is required for order item");
                }
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Quantity must be positive number");
                }
            });
        });
    }

    private Mono<Void> saveOrderItems(OrderDto orderDto, Integer orderId) {
        return Flux.fromIterable(orderDto.getOrderItemDtos())
                .map(orderItemDto -> {
                    // Создаем OrderItem с ссылкой на order
                    Order orderRef = Order.builder().orderId(orderId).build();
                    return OrderItem.builder()
                            .productId(orderItemDto.getProductId())
                            .quantity(orderItemDto.getQuantity())
                            .price(orderItemDto.getPrice())
                            .totalPrice(orderItemDto.getTotalPrice())
                            .orderId(orderId) // Устанавливаем foreign key
                            .build();
                })
                .flatMap(orderItemRepository::save)
                .then()
                .doOnSuccess(v ->
                        log.info("Saved {} order items for order: {}",
                                orderDto.getOrderItemDtos().size(), orderId))
                .doOnError(error ->
                        log.error("Error saving order items for order {}: {}",
                                orderId, error.getMessage()));
    }

    private void sendOrderConfirmationEmail(OrderDto orderDto, String jwtToken) {
        if (jwtToken == null || jwtToken.trim().isEmpty()) {
            log.warn("Cannot send confirmation email: JWT token is missing for order {}",
                    orderDto.getOrderId());
            return;
        }

        emailService.sendOrderConfirmationEmail(orderDto, jwtToken)
                .subscribe(
                        null,
                        error -> log.error("Failed to send confirmation email for order {}: {}",
                                orderDto.getOrderId(), error.getMessage())
                );
    }

    @Override
    @Transactional
    public Mono<OrderDto> update(final OrderDto orderDto) {
        log.info("Updating order: {}", orderDto.getOrderId());

        return findById(orderDto.getOrderId())
                .flatMap(existingOrder -> {
                    // Освобождаем старые резервации
                    return inventoryService.releaseProducts(existingOrder)
                            .then(Mono.defer(() -> {
                                // Обновляем заказ
                                Order order = OrderMappingHelper.mapToEntity(orderDto);
                                return orderRepository.save(order)
                                        .flatMap(savedOrder -> {
                                            // Удаляем старые items
                                            return orderItemRepository.deleteAllByOrderId(savedOrder.getOrderId())
                                                    .then(Mono.defer(() -> {
                                                        // Сохраняем новые items
                                                        return saveOrderItems(orderDto, savedOrder.getOrderId())
                                                                .then(Mono.defer(() ->
                                                                        // Резервируем товары по новому заказу
                                                                        inventoryService.reserveProducts(orderDto)
                                                                ))
                                                                .flatMap(reservationSuccess -> {
                                                                    if (!reservationSuccess) {
                                                                        return Mono.error(new RuntimeException(
                                                                                "Failed to reserve products after update"));
                                                                    }
                                                                    return findById(savedOrder.getOrderId());
                                                                });
                                                    }));
                                        });
                            }));
                })
                .doOnSuccess(updatedOrder ->
                        log.info("Successfully updated order: {}", updatedOrder.getOrderId()))
                .doOnError(error ->
                        log.error("Error updating order {}: {}", orderDto.getOrderId(), error.getMessage()));
    }

    @Override
    @Transactional
    public Mono<OrderDto> update(final Integer orderId, final OrderDto orderDto) {
        log.info("Updating order {} with new data", orderId);

        return findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(
                        String.format("Order with id: %d not found", orderId))))
                .flatMap(existingOrder -> {
                    // Обновляем только разрешенные поля
                    existingOrder.setOrderDesc(orderDto.getOrderDesc());
                    existingOrder.setStatus(orderDto.getStatus());

                    // Если изменились items, нужно обновить резервацию
                    if (!areOrderItemsEqual(existingOrder.getOrderItemDtos(), orderDto.getOrderItemDtos())) {
                        return updateOrderWithNewItems(existingOrder, orderDto);
                    } else {
                        // Только обновление метаданных заказа
                        Order order = OrderMappingHelper.mapToEntity(existingOrder);
                        return orderRepository.save(order)
                                .flatMap(savedOrder -> findById(savedOrder.getOrderId()));
                    }
                });
    }

    private boolean areOrderItemsEqual(Set<OrderItemDto> items1, Set<OrderItemDto> items2) {
        if (items1 == null && items2 == null) return true;
        if (items1 == null || items2 == null) return false;
        if (items1.size() != items2.size()) return false;

        // Проверяем совпадение productId и quantity
        return items1.stream()
                .allMatch(item1 -> items2.stream()
                        .anyMatch(item2 ->
                                item1.getProductId().equals(item2.getProductId()) &&
                                        item1.getQuantity().equals(item2.getQuantity())
                        )
                );
    }

    private Mono<OrderDto> updateOrderWithNewItems(OrderDto existingOrder, OrderDto newOrderDto) {
        return inventoryService.releaseProducts(existingOrder)
                .then(Mono.defer(() -> {
                    // Обновляем items
                    existingOrder.setOrderItemDtos(newOrderDto.getOrderItemDtos());
                    inventoryService.calculateOrderTotal(existingOrder);

                    return inventoryService.reserveProducts(existingOrder)
                            .flatMap(reservationSuccess -> {
                                if (!reservationSuccess) {
                                    return Mono.error(new RuntimeException(
                                            "Failed to reserve products with new items"));
                                }

                                // Сохраняем обновленный заказ
                                Order order = OrderMappingHelper.mapToEntity(existingOrder);
                                return orderRepository.save(order)
                                        .flatMap(savedOrder ->
                                                orderItemRepository.deleteAllByOrderId(savedOrder.getOrderId())
                                                        .then(Mono.defer(() ->
                                                                saveOrderItems(existingOrder, savedOrder.getOrderId())
                                                        ))
                                                        .then(Mono.defer(() ->
                                                                findById(savedOrder.getOrderId())
                                                        ))
                                        );
                            });
                }));
    }

    @Override
    @Transactional
    public Mono<Void> deleteById(final Integer orderId) {
        log.info("Deleting order: {}", orderId);

        return findById(orderId)
                .flatMap(orderDto ->
                        inventoryService.releaseProducts(orderDto)
                                .then(Mono.defer(() ->
                                        orderItemRepository.deleteAllByOrderId(orderId)
                                ))
                                .then(Mono.defer(() ->
                                        orderRepository.deleteById(orderId)
                                ))
                )
                .doOnSuccess(v ->
                        log.info("Successfully deleted order: {}", orderId))
                .doOnError(error ->
                        log.error("Error deleting order {}: {}", orderId, error.getMessage()))
                .then();
    }

    @Override
    public Mono<Boolean> existsByOrderId(Integer orderId) {
        return orderRepository.existsByOrderId(orderId)
                .doOnSuccess(exists ->
                        log.debug("Order {} exists: {}", orderId, exists))
                .doOnError(error ->
                        log.error("Error checking if order {} exists: {}", orderId, error.getMessage()));
    }
}