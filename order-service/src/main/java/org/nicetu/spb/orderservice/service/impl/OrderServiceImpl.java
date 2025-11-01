package org.nicetu.spb.orderservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.nicetu.spb.orderservice.exception.wrapper.OrderNotFoundException;
import org.nicetu.spb.orderservice.mapper.OrderMappingHelper;
import org.nicetu.spb.orderservice.model.dto.order.OrderDto;
import org.nicetu.spb.orderservice.model.dto.order.OrderItemDto;
import org.nicetu.spb.orderservice.model.entity.Order;
import org.nicetu.spb.orderservice.repository.OrderRepository;
import org.nicetu.spb.orderservice.security.JwtProvider;
import org.nicetu.spb.orderservice.service.CallAPI;
import org.nicetu.spb.orderservice.service.EmailService;
import org.nicetu.spb.orderservice.service.InventoryService;
import org.nicetu.spb.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private final OrderRepository orderRepository;

    @Autowired
    private final CallAPI callAPI;

    @Autowired
    private final InventoryService inventoryService;

    @Autowired
    private final EmailService emailService;

    @Autowired
    private final JwtProvider jwtProvider;

    @Override
    public Mono<List<OrderDto>> findAll() {
        log.info("OrderDto List, service; fetch all orders");
        return Mono.fromSupplier(() -> orderRepository.findAll()
                        .stream()
                        .map(OrderMappingHelper::mapToDto)
                        .toList())
                .flatMap(listOrderDtos -> Flux.fromIterable(listOrderDtos)
                        .flatMap(orderDto -> {
                            if (orderDto.getOrderItemDtos() != null) {
                                return Flux.fromIterable(orderDto.getOrderItemDtos())
                                        .flatMap(orderItem ->
                                                callAPI.receiverProductDto(orderItem.getProductId())
                                                        .map(productDto -> {
                                                            orderItem.setProductDto(productDto);
                                                            return orderItem;
                                                        })
                                                        .onErrorResume(throwable -> {
                                                            log.error("Error fetching product info for product {}: {}",
                                                                    orderItem.getProductId(), throwable.getMessage());
                                                            return Mono.just(orderItem);
                                                        })
                                        )
                                        .collectList()
                                        .map(orderItems -> {
                                            orderDto.setOrderItemDtos(orderItems.stream().collect(Collectors.toSet()));
                                            return orderDto;
                                        });
                            }
                            return Mono.just(orderDto);
                        })
                        .collectList());
    }

    @Override
    public Mono<Page<OrderDto>> findAll(int page, int size, String sortBy, String sortOrder) {
        log.info("OrderDto List, service; fetch all orders with paging and sorting");
        Sort sort = Sort.by(Sort.Direction.fromString(sortOrder), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return Mono.fromSupplier(() -> orderRepository.findAll(pageable)
                        .map(OrderMappingHelper::mapToDto)
                )
                .flatMap(orderPage -> Flux.fromIterable(orderPage)
                        .flatMap(orderDto -> {
                            if (orderDto.getOrderItemDtos() != null) {
                                return Flux.fromIterable(orderDto.getOrderItemDtos())
                                        .flatMap(orderItem ->
                                                callAPI.receiverProductDto(orderItem.getProductId())
                                                        .map(productDto -> {
                                                            orderItem.setProductDto(productDto);
                                                            return orderItem;
                                                        })
                                                        .onErrorResume(throwable -> {
                                                            log.error("Error fetching product info for product {}: {}",
                                                                    orderItem.getProductId(), throwable.getMessage());
                                                            return Mono.just(orderItem);
                                                        })
                                        )
                                        .collectList()
                                        .map(orderItems -> {
                                            orderDto.setOrderItemDtos(orderItems.stream().collect(Collectors.toSet()));
                                            return orderDto;
                                        });
                            }
                            return Mono.just(orderDto);
                        })
                        .collectList()
                        .map(resultList -> new PageImpl<>(resultList, pageable, orderPage.getTotalElements()))
                );
    }

    @Override
    public Mono<OrderDto> findById(Integer orderId) {
        log.info("OrderDto, service; fetch order by id");
        return Mono.fromSupplier(() -> orderRepository.findById(orderId)
                        .map(OrderMappingHelper::mapToDto)
                        .orElseThrow(() -> new OrderNotFoundException(String.format("Order with id: %d not found", orderId)))
                )
                .flatMap(orderDto -> {
                    if (orderDto.getOrderItemDtos() != null) {
                        return Flux.fromIterable(orderDto.getOrderItemDtos())
                                .flatMap(orderItem ->
                                        callAPI.receiverProductDto(orderItem.getProductId())
                                                .map(productDto -> {
                                                    orderItem.setProductDto(productDto);
                                                    return orderItem;
                                                })
                                                .onErrorResume(throwable -> {
                                                    log.error("Error fetching product info for product {}: {}",
                                                            orderItem.getProductId(), throwable.getMessage());
                                                    return Mono.just(orderItem);
                                                })
                                )
                                .collectList()
                                .map(orderItems -> {
                                    orderDto.setOrderItemDtos(orderItems.stream().collect(Collectors.toSet()));
                                    return orderDto;
                                });
                    }
                    return Mono.just(orderDto);
                });
    }


    @Override
    public Boolean existsByOrderId(Integer orderId) {
        return orderRepository.findById(orderId).isPresent();
    }

    @Override
    @Transactional
    public Mono<OrderDto> save(final OrderDto orderDto, final String jwtToken) {
        log.info("OrderDto, service; save order with multiple items");

        if (orderDto == null) {
            return Mono.error(new IllegalArgumentException("OrderDto cannot be null"));
        }

        if (orderDto.getOrderItemDtos() == null || orderDto.getOrderItemDtos().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Order must have at least one item"));
        }

        for (OrderItemDto item : orderDto.getOrderItemDtos()) {
            if (item == null || item.getProductId() == null || item.getQuantity() == null) {
                return Mono.error(new IllegalArgumentException("Order items must have productId and quantity"));
            }
            if (item.getPrice() == null) {
                log.warn("Price is null for productId: {}, setting to 0.0", item.getProductId());
                item.setPrice(0.0);
            }
        }

        inventoryService.calculateOrderTotal(orderDto);
        orderDto.setStatus("NEW");

        return inventoryService.reserveProducts(orderDto)
                .then(Mono.fromSupplier(() -> {
                    Order order = OrderMappingHelper.mapToEntity(orderDto);

                    if (order.getOrderItems() != null) {
                        order.getOrderItems().forEach(orderItem -> orderItem.setOrder(order));
                    }

                    Order savedOrder = orderRepository.save(order);
                    return OrderMappingHelper.mapToDto(savedOrder);
                }))
                .doOnSuccess(savedOrder -> {
                    sendOrderConfirmationEmailAsync(savedOrder, jwtToken);
                })
                .onErrorResume(throwable -> {
                    log.error("Error saving order: {}", throwable.getMessage());
                    return Mono.error(throwable);
                });
    }

    private void sendOrderConfirmationEmailAsync(OrderDto orderDto, String jwtToken) {
        if (jwtToken == null) {
            log.warn("JWT token is null for order: {}", orderDto.getOrderId());
            return;
        }

        String customerEmail = jwtProvider.getEmailFromToken(jwtToken);
        if (customerEmail == null) {
            log.warn("Cannot extract email from token for order: {}", orderDto.getOrderId());
            return;
        }

        log.info("Sending email to: {} for order: {}", customerEmail, orderDto.getOrderId());

        emailService.sendOrderConfirmationEmail(orderDto, jwtToken)
                .subscribe(
                        null,
                        error -> log.error("Failed to send order confirmation email for order: {}", orderDto.getOrderId(), error)
                );
    }

    @Override
    public Mono<OrderDto> update(final OrderDto orderDto) {
        log.info("OrderDto, service; update order");

        return findById(orderDto.getOrderId())
                .flatMap(existingOrderDto -> {
                    return inventoryService.releaseProducts(existingOrderDto)
                            .then(inventoryService.reserveProducts(orderDto))
                            .then(Mono.fromSupplier(() -> {
                                Order order = OrderMappingHelper.mapToEntity(orderDto);

                                if (order.getOrderItems() != null) {
                                    order.getOrderItems().forEach(orderItem -> orderItem.setOrder(order));
                                }

                                Order savedOrder = orderRepository.save(order);
                                return OrderMappingHelper.mapToDto(savedOrder);
                            }));
                });
    }

    @Override
    public Mono<OrderDto> update(final Integer orderId, final OrderDto orderDto) {
        log.info("OrderDto, service; update order with orderId");

        return findById(orderId)
                .flatMap(existingOrderDto -> {
                    existingOrderDto.setOrderDate(orderDto.getOrderDate());
                    existingOrderDto.setOrderDesc(orderDto.getOrderDesc());
                    existingOrderDto.setStatus(orderDto.getStatus());

                    if (!existingOrderDto.getOrderItemDtos().equals(orderDto.getOrderItemDtos())) {
                        return inventoryService.releaseProducts(existingOrderDto)
                                .then(Mono.defer(() -> {
                                    existingOrderDto.setOrderItemDtos(orderDto.getOrderItemDtos());
                                    inventoryService.calculateOrderTotal(existingOrderDto);
                                    return inventoryService.reserveProducts(existingOrderDto);
                                }))
                                .then(Mono.fromSupplier(() -> {
                                    Order order = OrderMappingHelper.mapToEntity(existingOrderDto);

                                    if (order.getOrderItems() != null) {
                                        order.getOrderItems().forEach(orderItem -> orderItem.setOrder(order));
                                    }

                                    Order savedOrder = orderRepository.save(order);
                                    return OrderMappingHelper.mapToDto(savedOrder);
                                }));
                    } else {
                        return Mono.fromSupplier(() -> {
                            Order order = OrderMappingHelper.mapToEntity(existingOrderDto);
                            Order savedOrder = orderRepository.save(order);
                            return OrderMappingHelper.mapToDto(savedOrder);
                        });
                    }
                })
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order with id " + orderId + " not found")));
    }

    @Override
    public Mono<Void> deleteById(final Integer orderId) {
        log.info("Void, service; delete order by id");

        return findById(orderId)
                .flatMap(orderDto -> {
                    return inventoryService.releaseProducts(orderDto)
                            .then(Mono.fromRunnable(() -> orderRepository.deleteById(orderId)));
                });
    }
}