package org.nicetu.spb.orderservice.repository;

import org.nicetu.spb.orderservice.model.entity.Order;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, Integer> {

    @Query("DELETE FROM orders WHERE cart_id = :cartId")
    Mono<Void> deleteAllByCartId(Integer cartId);

    @Query("SELECT * FROM orders WHERE cart_id = :cartId")
    Flux<Order> findAllByCartId(Integer cartId);

    @Query("SELECT EXISTS(SELECT 1 FROM orders WHERE order_id = :orderId)")
    Mono<Boolean> existsByOrderId(Integer orderId);

    @Query("SELECT * FROM orders ORDER BY :sortBy :sortDirection LIMIT :limit OFFSET :offset")
    Flux<Order> findAllWithPagination(String sortBy, String sortDirection, int limit, int offset);

    @Query("SELECT COUNT(*) FROM orders")
    Mono<Long> countAll();
}