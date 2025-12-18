package org.nicetu.spb.productservice.repository;

import org.nicetu.spb.productservice.model.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductRepository extends R2dbcRepository<Product, Long> {

    Flux<Product> findAllBy(Pageable pageable);

    @Query("""
        UPDATE products 
        SET quantity = quantity - :quantity, 
            version = version + 1
        WHERE product_id = :productId 
        AND quantity >= :quantity
        RETURNING *
        """)
    Mono<Product> reserveQuantity(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );

    @Query("""
        UPDATE products 
        SET quantity = quantity + :quantity,
            version = version + 1
        WHERE product_id = :productId
        RETURNING *
        """)
    Mono<Product> incrementQuantity(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );

    @Query("""
        SELECT p.* FROM products p
        WHERE p.quantity >= :minQuantity
        ORDER BY p.product_id
        """)
    Flux<Product> findAvailableProducts(@Param("minQuantity") Integer minQuantity, Pageable pageable);

    @Query("""
        SELECT p.* FROM products p
        JOIN product_categories pc ON p.product_id = pc.product_id
        WHERE pc.category_id = :categoryId
        """)
    Flux<Product> findByCategoryId(@Param("categoryId") Integer categoryId, Pageable pageable);

    @Query("""
        SELECT category_id FROM product_categories 
        WHERE product_id = :productId
        """)
    Flux<Long> findCategoryIdsByProductId(@Param("productId") Long productId);
}