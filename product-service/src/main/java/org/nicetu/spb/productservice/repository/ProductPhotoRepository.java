package org.nicetu.spb.productservice.repository;

import org.nicetu.spb.productservice.model.entity.ProductPhoto;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductPhotoRepository extends R2dbcRepository<ProductPhoto, Long> {

    Flux<ProductPhoto> findByProductId(Long productId);

    @Modifying
    @Query("DELETE FROM product_photo WHERE product_id = :productId")
    Mono<Integer> deleteByProductId(@Param("productId") Long productId);
}