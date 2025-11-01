package org.nicetu.spb.productservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/inventory")
public class ProductInventoryController {

    private final ProductService productService;

    @PatchMapping("/{productId}/quantity")
    public ResponseEntity<Boolean> updateQuantity(
            @PathVariable("productId") Long productId,
            @RequestParam Integer quantity) {
        log.info("Updating quantity for product {} to {}", productId, quantity);

        try {
            ProductDto productDto = productService.findById(productId);
            productDto.setQuantity(quantity);

            ProductDto updatedProduct = productService.update(productDto);
            return ResponseEntity.ok(true);  // ← Возвращать true при успехе
        } catch (Exception e) {
            log.error("Error updating product quantity: {}", e.getMessage());
            return ResponseEntity.ok(false);  // ← Возвращать false при ошибке
        }
    }

    @PostMapping("/{productId}/reserve")
    public ResponseEntity<ProductDto> reserveProduct(
            @PathVariable("productId") Long productId,
            @RequestParam Integer quantity) {
        log.info("Reserving {} units of product {}", quantity, productId);

        ProductDto productDto = productService.findById(productId);

        if (productDto.getQuantity() < quantity) {
            return ResponseEntity.badRequest().body(null);
        }

        productDto.setQuantity(productDto.getQuantity() - quantity);
        ProductDto updatedProduct = productService.update(productDto);

        log.info("Product {} reserved. New quantity: {}", productId, updatedProduct.getQuantity());
        return ResponseEntity.ok(updatedProduct);
    }

    @PostMapping("/{productId}/release")
    public ResponseEntity<ProductDto> releaseProduct(
            @PathVariable("productId") Long productId,
            @RequestParam Integer quantity) {
        log.info("Releasing {} units of product {}", quantity, productId);

        ProductDto productDto = productService.findById(productId);
        productDto.setQuantity(productDto.getQuantity() + quantity);

        ProductDto updatedProduct = productService.update(productDto);

        log.info("Product {} released. New quantity: {}", productId, updatedProduct.getQuantity());
        return ResponseEntity.ok(updatedProduct);
    }

    @GetMapping("/{productId}/availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @PathVariable("productId") Long productId,
            @RequestParam Integer requestedQuantity) {
        log.info("Checking availability for product {}, quantity {}", productId, requestedQuantity);

        ProductDto productDto = productService.findById(productId);

        boolean available = productDto.getQuantity() >= requestedQuantity;
        Map<String, Object> response = Map.of(
                "productId", productId,
                "requestedQuantity", requestedQuantity,
                "availableQuantity", productDto.getQuantity(),
                "isAvailable", available,
                "canFulfill", available
        );

        return ResponseEntity.ok(response);
    }
}