package org.nicetu.spb.productservice.mapper;

import org.nicetu.spb.productservice.model.dto.ProductPhotoDto;
import org.nicetu.spb.productservice.model.entity.ProductPhoto;
import org.springframework.stereotype.Component;

@Component
public class ProductPhotoMapping {

    public static ProductPhotoDto mapToDto(ProductPhoto photo) {
        if (photo == null) return null;

        return ProductPhotoDto.builder()
                .photoId(photo.getPhotoId())
                .photoLink(photo.getPhotoLink())
                .productDto(photo.getProduct() != null ?
                        ProductMapping.mapToDto(photo.getProduct()) : null)
                .build();
    }

    public static ProductPhoto mapToEntity(ProductPhotoDto photoDto) {
        if (photoDto == null) return null;

        return ProductPhoto.builder()
                .photoId(photoDto.getPhotoId())
                .photoLink(photoDto.getPhotoLink())
                .productId(photoDto.getProductDto() != null ?
                        photoDto.getProductDto().getProductId() : null)
                .build();
    }
}