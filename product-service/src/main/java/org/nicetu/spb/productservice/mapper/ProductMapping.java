package org.nicetu.spb.productservice.mapper;

import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.model.entity.Product;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ProductMapping {

    public static ProductDto mapToDto(Product product) {
        if (product == null) return null;

        return ProductDto.builder()
                .productId(product.getProductId())
                .title(product.getTitle())
                .description(product.getDescription())
                .quantity(product.getQuantity())
                .quantityStatus(product.getQuantityStatus())
                .priceUnit(product.getPriceUnit())
                .discount(product.getDiscount())
                .categories(product.getCategories() != null ?
                        product.getCategories().stream()
                                .map(CategoryMapping::mapToDto)
                                .collect(Collectors.toSet()) : null)
                .productPhotos(product.getProductPhotos() != null ?
                        product.getProductPhotos().stream()
                                .map(ProductPhotoMapping::mapToDto)
                                .collect(Collectors.toList()) : null)
                .build();
    }

    public static Product mapToEntity(ProductDto productDto) {
        if (productDto == null) return null;

        Product product = Product.builder()
                .productId(productDto.getProductId())
                .title(productDto.getTitle())
                .description(productDto.getDescription())
                .quantity(productDto.getQuantity())
                .priceUnit(productDto.getPriceUnit())
                .discount(productDto.getDiscount())
                .build();

        product.calculateQuantityStatus();

        if (productDto.getCategories() != null) {
            product.setCategories(
                    productDto.getCategories().stream()
                            .map(CategoryMapping::mapToEntity)
                            .collect(Collectors.toSet())
            );
        }

        return product;
    }
}