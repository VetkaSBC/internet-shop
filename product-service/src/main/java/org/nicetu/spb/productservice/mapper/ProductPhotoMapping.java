package org.nicetu.spb.productservice.mapper;

import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.model.dto.ProductPhotoDto;
import org.nicetu.spb.productservice.model.entity.Category;
import org.nicetu.spb.productservice.model.entity.Product;
import org.nicetu.spb.productservice.model.entity.ProductPhoto;

public interface ProductPhotoMapping {
    static ProductPhotoDto mapToDto(final ProductPhoto productPhoto) {
        return ProductPhotoDto.builder()
                .photoId(productPhoto.getPhotoId())
                .productDto(
                        ProductDto.builder()
                                .productId(productPhoto.getProduct().getProductId())
                                .title(productPhoto.getProduct().getTitle())
                                .description(productPhoto.getProduct().getDescription())
                                .quantity(productPhoto.getProduct().getQuantity())
                                .price(productPhoto.getProduct().getPrice())
                                .discount(productPhoto.getProduct().getDiscount())
                                .categoryDto(
                                        CategoryDto.builder()
                                                .categoryId(productPhoto.getProduct().getCategory().getCategoryId())
                                                .categoryTitle(productPhoto.getProduct().getCategory().getCategoryTitle())
                                                .build())
                                .build())
                .photoLink(productPhoto.getPhotoLink())
                .build();
    }

    static ProductPhoto mapToEntity(ProductPhotoDto productPhotoDto) {
        return ProductPhoto.builder()
                .photoId(productPhotoDto.getPhotoId())
                .product(
                        Product.builder()
                                .productId(productPhotoDto.getProductDto().getProductId())
                                .title(productPhotoDto.getProductDto().getTitle())
                                .description(productPhotoDto.getProductDto().getDescription())
                                .quantity(productPhotoDto.getProductDto().getQuantity())
                                .price(productPhotoDto.getProductDto().getPrice())
                                .discount(productPhotoDto.getProductDto().getDiscount())
                                .category(
                                        Category.builder()
                                                .categoryId(productPhotoDto.getProductDto().getCategoryDto().getCategoryId())
                                                .categoryTitle(productPhotoDto.getProductDto().getCategoryDto().getCategoryTitle())
                                                .build())
                                .build())
                .photoLink(productPhotoDto.getPhotoLink())
                .build();
    }
}