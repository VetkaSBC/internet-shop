package org.nicetu.spb.orderservice.mapper;


import org.nicetu.spb.orderservice.model.dto.order.CartDto;
import org.nicetu.spb.orderservice.model.dto.order.OrderDto;
import org.nicetu.spb.orderservice.model.entity.Cart;
import org.nicetu.spb.orderservice.model.entity.Order;

import java.util.Collections;
import java.util.stream.Collectors;

public interface OrderMappingHelper {
    static OrderDto mapToDto(Order order) {
        if (order == null) return null;

        CartDto cartDto = null;
        if (order.getCart() != null) {
            cartDto = CartDto.builder()
                    .cartId(order.getCart().getCartId())
                    .userId(order.getCart().getUserId())
                    .build();
        }

        return OrderDto.builder()
                .orderId(order.getOrderId())
                .orderDate(order.getOrderDate())
                .orderDesc(order.getOrderDesc())
                .orderFee(order.getOrderFee())
                .status(order.getStatus())
                .orderItemDtos(order.getOrderItems() != null ?
                        order.getOrderItems().stream()
                                .map(OrderItemMappingHelper::mapToDto)
                                .collect(Collectors.toSet()) :
                        Collections.emptySet())
                .cartDto(cartDto)
                .build();
    }

    static Order mapToEntity(final OrderDto orderDto) {
        if (orderDto == null) return null;

        Cart cart = null;
        if (orderDto.getCartDto() != null && orderDto.getCartDto().getCartId() != null) {
            cart = Cart.builder()
                    .cartId(orderDto.getCartDto().getCartId())
                    .userId(orderDto.getCartDto().getUserId())
                    .build();
        }

        return Order.builder()
                .orderId(orderDto.getOrderId())
                .orderDate(orderDto.getOrderDate())
                .orderDesc(orderDto.getOrderDesc())
                .orderFee(orderDto.getOrderFee())
                .status(orderDto.getStatus())
                .orderItems(orderDto.getOrderItemDtos() != null ?
                        orderDto.getOrderItemDtos().stream()
                                .map(OrderItemMappingHelper::mapToEntity)
                                .collect(Collectors.toSet()) :
                        Collections.emptySet())
                .cart(cart)
                .build();
    }
}