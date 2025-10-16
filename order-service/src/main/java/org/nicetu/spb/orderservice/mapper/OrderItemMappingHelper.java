package org.nicetu.spb.orderservice.mapper;

import org.nicetu.spb.orderservice.model.dto.order.OrderDto;
import org.nicetu.spb.orderservice.model.dto.order.OrderItemDto;
import org.nicetu.spb.orderservice.model.entity.Order;
import org.nicetu.spb.orderservice.model.entity.OrderItem;

public interface OrderItemMappingHelper {
    static OrderItemDto mapToDto(OrderItem orderItem) {
        if (orderItem == null) return null;
        return OrderItemDto.builder()
                .orderItemId(orderItem.getOrderItemId())
                .productId(orderItem.getProductId())
                .quantity(orderItem.getQuantity())
                .price(orderItem.getPrice())
                .totalPrice(orderItem.getTotalPrice())
                .orderDto(OrderDto.builder()
                        .orderId(orderItem.getOrder().getOrderId())
                        .build())
                .build();
    }

    static OrderItem mapToEntity(final OrderItemDto orderItemDto) {
        if (orderItemDto == null) return null;

        Order order = null;
        if (orderItemDto.getOrderDto() != null && orderItemDto.getOrderDto().getOrderId() != null) {
            order = Order.builder()
                    .orderId(orderItemDto.getOrderDto().getOrderId())
                    .build();
        }

        return OrderItem.builder()
                .orderItemId(orderItemDto.getOrderItemId())
                .productId(orderItemDto.getProductId())
                .quantity(orderItemDto.getQuantity())
                .price(orderItemDto.getPrice())
                .totalPrice(orderItemDto.getTotalPrice())
                .order(order)
                .build();
    }
}