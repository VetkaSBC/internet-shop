package org.nicetu.spb.orderservice.mapper;

import org.nicetu.spb.orderservice.model.dto.order.CartDto;
import org.nicetu.spb.orderservice.model.dto.order.OrderDto;
import org.nicetu.spb.orderservice.model.dto.order.OrderItemDto;
import org.nicetu.spb.orderservice.model.entity.Cart;
import org.nicetu.spb.orderservice.model.entity.Order;
import org.nicetu.spb.orderservice.model.entity.OrderItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

public interface OrderMappingHelper {

    static Mono<OrderDto> mapToDto(Order order, Flux<OrderItem> orderItems, Mono<Cart> cart) {
        if (order == null) return Mono.empty();

        return Mono.zip(
                orderItems.collectList(),
                cart.defaultIfEmpty(null)
        ).map(tuple -> {
            var items = tuple.getT1();
            var cartEntity = tuple.getT2();

            CartDto cartDto = null;
            if (cartEntity != null) {
                cartDto = CartDto.builder()
                        .cartId(cartEntity.getCartId())
                        .userId(cartEntity.getUserId())
                        .build();
            }

            return OrderDto.builder()
                    .orderId(order.getOrderId())
                    .orderDate(order.getOrderDate())
                    .orderDesc(order.getOrderDesc())
                    .orderFee(order.getOrderFee())
                    .status(order.getStatus())
                    .orderItemDtos(items.stream()
                            .map(item -> OrderItemDto.builder()
                                    .orderItemId(item.getOrderItemId())
                                    .productId(item.getProductId())
                                    .quantity(item.getQuantity())
                                    .price(item.getPrice())
                                    .totalPrice(item.getTotalPrice())
                                    .build())
                            .collect(Collectors.toSet()))
                    .cartDto(cartDto)
                    .build();
        });
    }

    static Order mapToEntity(final OrderDto orderDto) {
        if (orderDto == null) return null;

        return Order.builder()
                .orderId(orderDto.getOrderId())
                .orderDate(orderDto.getOrderDate())
                .orderDesc(orderDto.getOrderDesc())
                .orderFee(orderDto.getOrderFee())
                .status(orderDto.getStatus())
                .cartId(orderDto.getCartDto() != null ? orderDto.getCartDto().getCartId() : null)
                .build();
    }
}