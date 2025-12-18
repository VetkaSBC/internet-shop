package org.nicetu.spb.orderservice.repository;

import org.nicetu.spb.orderservice.model.entity.OrderItem;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderItemRepository extends R2dbcRepository<OrderItem, Integer> {

    @Query("SELECT * FROM order_items WHERE order_id = :orderId")
    Flux<OrderItem> findAllByOrderId(Integer orderId);

    @Query("DELETE FROM order_items WHERE order_id = :orderId")
    Mono<Void> deleteAllByOrderId(Integer orderId);
}