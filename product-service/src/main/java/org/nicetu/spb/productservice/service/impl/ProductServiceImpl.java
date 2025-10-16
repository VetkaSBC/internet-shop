package org.nicetu.spb.productservice.service.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.productservice.exception.wrapper.ProductNotFoundException;
import org.nicetu.spb.productservice.mapper.CategoryMapping;
import org.nicetu.spb.productservice.mapper.ProductMapping;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.model.entity.Product;
import org.nicetu.spb.productservice.repository.ProductRepository;
import org.nicetu.spb.productservice.service.ProductService;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public Flux<List<ProductDto>> findAll() {
        log.info("ProductDto List, service, fetch all products");
        return Flux.defer(() -> {
                    List<ProductDto> productDtos = productRepository.findAll()
                            .stream()
                            .map(ProductMapping::mapToDto)
                            .distinct()
                            .toList();
                    return Flux.just(productDtos);
                })
                .onErrorResume(throwable -> {
                    log.error("Error while fetching products: " + throwable.getMessage());
                    return Flux.empty();
                });
    }

    @Override
    public ProductDto findById(Long productId) {
        log.info("ProductDto, service; fetch product by id");
        return productRepository.findById(productId)
                .map(ProductMapping::mapToDto)
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product with id[%d] not found", productId)));
    }

    @Override
    public ProductDto save(final ProductDto productDto) {
        log.info("ProductDto, service; save product");
        try {
            Product product = ProductMapping.mapToEntity(productDto);
            product.calculateQuantityStatus();

            Product savedProduct = productRepository.save(product);
            return ProductMapping.mapToDto(savedProduct);
        } catch (DataIntegrityViolationException e) {
            log.error("Error saving product: Data integrity violation", e);
            throw new ProductNotFoundException("Error saving product: Data integrity violation", e);
        } catch (Exception e) {
            log.error("Error saving product", e);
            throw new ProductNotFoundException("Error saving product", e);
        }
    }

    @Override
    public ProductDto update(final ProductDto productDto) {
        log.info("ProductDto, service; update product");

        Product existingProduct = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productDto.getProductId()));

        Long existingId = existingProduct.getProductId();

        BeanUtils.copyProperties(productDto, existingProduct, "productId", "category", "productPhotos");

        existingProduct.setProductId(existingId);

        if (productDto.getCategoryDto() != null) {
            existingProduct.setCategory(CategoryMapping.mapToEntity(productDto.getCategoryDto()));
        }
        existingProduct.calculateQuantityStatus();

        Product updatedProduct = productRepository.save(existingProduct);
        return ProductMapping.mapToDto(updatedProduct);
    }

    @Override
    public ProductDto update(Long productId, ProductDto productDto) {
        log.info("ProductDto, service; update product with productId");

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        BeanUtils.copyProperties(productDto, existingProduct, "productId", "category");

        if (productDto.getCategoryDto() != null) {
            existingProduct.setCategory(CategoryMapping.mapToEntity(productDto.getCategoryDto()));
        }

        Product updatedProduct = productRepository.save(existingProduct);

        return ProductMapping.mapToDto(updatedProduct);
    }

    @Override
    public void deleteById(Long productId) {
        log.info("Void, service; delete product by id");
        this.productRepository.delete(ProductMapping.mapToEntity(this.findById(productId)));
    }

    public boolean isProductAvailable(Long productId, Integer quantity) {
        ProductDto productDto = this.findById(productId);
        return productDto.getQuantity() >= quantity;
    }

    public ProductDto reserveProduct(Long productId, Integer quantity) {
        ProductDto productDto = this.findById(productId);

        if (productDto.getQuantity() < quantity) {
            throw new ProductNotFoundException(
                    String.format("Not enough stock for product %d. Available: %d, Requested: %d",
                            productId, productDto.getQuantity(), quantity));
        }

        productDto.setQuantity(productDto.getQuantity() - quantity);
        return this.update(productDto);
    }

    public ProductDto releaseProduct(Long productId, Integer quantity) {
        ProductDto productDto = this.findById(productId);
        productDto.setQuantity(productDto.getQuantity() + quantity);
        return this.update(productDto);
    }

}
