package org.nicetu.spb.productservice.mapper;

import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.model.entity.Category;
import org.nicetu.spb.productservice.model.entity.Product;

public interface ProductMapping {
    static ProductDto mapToDto(Product product) {
        return ProductDto.builder()
                .productId(product.getProductId())
                .title(product.getTitle())
                .description(product.getDescription())
                .quantity(product.getQuantity())
                .quantityStatus(product.getQuantityStatus())
                .priceUnit(product.getPriceUnit()) // Добавлено
                .discount(product.getDiscount())
                .categoryDto(
                        CategoryDto.builder()
                                .categoryId(product.getCategory().getCategoryId())
                                .categoryTitle(product.getCategory().getCategoryTitle())
                                .build())
                .build();
    }

    static Product mapToEntity(ProductDto productDto) {
        return Product.builder()
                .productId(productDto.getProductId())
                .title(productDto.getTitle())
                .description(productDto.getDescription())
                .quantity(productDto.getQuantity())
                .priceUnit(productDto.getPriceUnit()) // Добавлено
                .discount(productDto.getDiscount())
                .category(
                        Category.builder()
                                .categoryId(productDto.getCategoryDto().getCategoryId())
                                .categoryTitle(productDto.getCategoryDto().getCategoryTitle())
                                .build())
                .build();
    }
}