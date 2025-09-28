package org.nicetu.spb.productservice.repository;

import org.nicetu.spb.productservice.model.entity.ProductPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductPhotoRepository extends JpaRepository<ProductPhoto, Long> {
    List<ProductPhoto> findByProductProductId(Long routeId);

    @Modifying
    @Query("DELETE FROM ProductPhoto p WHERE p.product.productId = :productId")
    void deleteByProductId(@Param("routeId") Long routeId);
}
