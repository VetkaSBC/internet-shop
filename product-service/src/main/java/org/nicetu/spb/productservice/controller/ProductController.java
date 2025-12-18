package org.nicetu.spb.productservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Mono<ResponseEntity<Page<ProductDto>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetch products with pagination: page={}, size={}", page, size);

        return productService.findAll(page, size)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @GetMapping("/{productId}")
    public Mono<ResponseEntity<ProductDto>> findById(
            @PathVariable("productId")
            @NotBlank(message = "Input must not be blank!")
            @Valid final String productId) {
        log.info("Fetch product by id: {}", productId);

        return productService.findById(Long.parseLong(productId))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<ProductDto>> save(
            @RequestBody
            @NotNull(message = "Input must not be NULL!")
            @Valid final ProductDto productDto) {
        log.info("Save product");

        return productService.save(productDto)
                .map(savedProduct -> ResponseEntity.status(201).body(savedProduct))
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @PutMapping
    public Mono<ResponseEntity<ProductDto>> update(
            @RequestBody
            @NotNull(message = "Input must not be NULL!")
            @Valid final ProductDto productDto) {
        log.info("Update product");

        return productService.update(productDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{productId}")
    public Mono<ResponseEntity<ProductDto>> update(
            @PathVariable("productId")
            @NotBlank(message = "Input must not be blank!")
            @Valid final String productId,
            @RequestBody
            @NotNull(message = "Input must not be NULL!")
            @Valid final ProductDto productDto) {
        log.info("Update product with productId: {}", productId);

        return productService.update(Long.parseLong(productId), productDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{productId}")
    public Mono<ResponseEntity<Void>> deleteById(
            @PathVariable("productId") final String productId) {
        log.info("Delete product by id: {}", productId);

        return productService.deleteById(Long.parseLong(productId))
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping("/{productId}/reserve")
    public Mono<ResponseEntity<ProductDto>> reserveProduct(
            @PathVariable("productId") Long productId,
            @RequestParam Integer quantity) {
        log.info("Reserve product: productId={}, quantity={}", productId, quantity);

        return productService.reserveProduct(productId, quantity)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/{productId}/release")
    public Mono<ResponseEntity<ProductDto>> releaseProduct(
            @PathVariable("productId") Long productId,
            @RequestParam Integer quantity) {
        log.info("Release product: productId={}, quantity={}", productId, quantity);

        return productService.releaseProduct(productId, quantity)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{productId}/available")
    public Mono<ResponseEntity<Boolean>> isProductAvailable(
            @PathVariable("productId") Long productId,
            @RequestParam Integer quantity) {
        log.info("Check product availability: productId={}, quantity={}", productId, quantity);

        return productService.isProductAvailable(productId, quantity)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(false));
    }
}