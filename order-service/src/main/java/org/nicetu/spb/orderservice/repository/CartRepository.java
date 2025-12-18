package org.nicetu.spb.orderservice.repository;

import org.nicetu.spb.orderservice.model.entity.Cart;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CartRepository extends R2dbcRepository<Cart, Integer> {

    @Query("DELETE FROM carts WHERE cart_id = :cartId")
    Mono<Void> deleteByCartId(Integer cartId);

    @Query("SELECT * FROM carts ORDER BY :sortBy :sortDirection LIMIT :limit OFFSET :offset")
    Flux<Cart> findAllWithPagination(String sortBy, String sortDirection, int limit, int offset);
}