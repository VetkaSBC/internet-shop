package org.nicetu.spb.productservice.repository;

import org.nicetu.spb.productservice.model.ProductCategory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductCategoryRepository extends R2dbcRepository<ProductCategory, Long> {

    @Query("DELETE FROM product_categories WHERE product_id = :productId")
    Mono<Integer> deleteByProductId(Long productId);

    @Query("DELETE FROM product_categories WHERE category_id = :categoryId")
    Mono<Integer> deleteByCategoryId(Integer categoryId);

    @Query("SELECT category_id FROM product_categories WHERE product_id = :productId")
    Flux<Long> findCategoryIdsByProductId(Long productId);

    @Query("SELECT product_id FROM product_categories WHERE category_id = :categoryId")
    Flux<Long> findProductIdsByCategoryId(Integer categoryId);
}