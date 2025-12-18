package org.nicetu.spb.orderservice.mapper;

import org.nicetu.spb.orderservice.model.dto.order.CartDto;
import org.nicetu.spb.orderservice.model.dto.order.OrderDto;
import org.nicetu.spb.orderservice.model.dto.user.UserDto;
import org.nicetu.spb.orderservice.model.entity.Cart;
import org.nicetu.spb.orderservice.model.entity.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

public interface CartMappingHelper {

    static Mono<CartDto> mapToDto(Cart cart, Flux<Order> orders) {
        if (cart == null) return Mono.empty();

        return orders.collectList()
                .map(orderList -> CartDto.builder()
                        .cartId(cart.getCartId())
                        .userId(cart.getUserId())
                        .orderDtos(orderList.stream()
                                .map(order -> OrderDto.builder()
                                        .orderId(order.getOrderId())
                                        .orderDate(order.getOrderDate())
                                        .orderDesc(order.getOrderDesc())
                                        .orderFee(order.getOrderFee())
                                        .status(order.getStatus())
                                        .build())
                                .collect(Collectors.toSet()))
                        .userDto(UserDto.builder()
                                .id(cart.getUserId())
                                .build())
                        .build());
    }

    static Cart mapToEntity(final CartDto cartDto) {
        if (cartDto == null) return null;

        return Cart.builder()
                .cartId(cartDto.getCartId())
                .userId(cartDto.getUserId())
                .build();
    }
}