package org.nicetu.spb.productservice.repository;

import org.nicetu.spb.productservice.model.entity.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CategoryRepository extends R2dbcRepository<Category, Integer> {

    Flux<Category> findAllBy(Pageable pageable);

    @Query("SELECT * FROM categories WHERE parent_category_id = :parentId")
    Flux<Category> findByParentCategoryId(@Param("parentId") Integer parentId);

    @Query("""
        SELECT c.* FROM categories c
        JOIN product_categories pc ON c.category_id = pc.category_id
        WHERE pc.product_id = :productId
        """)
    Flux<Category> findByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("DELETE FROM product_categories WHERE category_id = :categoryId")
    Mono<Integer> deleteProductCategoryRelations(@Param("categoryId") Integer categoryId);

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE parent_category_id = :categoryId)")
    Mono<Boolean> hasSubcategories(@Param("categoryId") Integer categoryId);
}