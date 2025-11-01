package org.nicetu.spb.productservice.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;



import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private final ProductService productService;

    @GetMapping
    public Flux<List<ProductDto>> findAll() {
        log.info("ProductDto List, controller; fetch all categories");
        return productService.findAll();
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto> findById(@PathVariable("productId")
                                               @NotBlank(message = "Input must not be blank!")
                                               @Valid final String productId) {
        log.info("ProductDto, resource; fetch product by id");
        return ResponseEntity.ok(productService.findById(Long.parseLong(productId)));
    }

    @PostMapping
    public ResponseEntity<ProductDto> save(@RequestBody
                                           @NotNull(message = "Input must not be NULL!")
                                           @Valid final ProductDto productDto) {
        log.info("ProductDto, resource; save product");
        return ResponseEntity.ok(productService.save(productDto));
    }

    @PutMapping
    public ResponseEntity<ProductDto> update(@RequestBody
                                             @NotNull(message = "Input must not be NULL!")
                                             @Valid final ProductDto productDto) {
        log.info("ProductDto, resource; update product");
        return ResponseEntity.ok(productService.update(productDto));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductDto> update(@PathVariable("productId")
                                             @NotBlank(message = "Input must not be blank!")
                                             @Valid final String productId,
                                             @RequestBody
                                             @NotNull(message = "Input must not be NULL!")
                                             @Valid final ProductDto productDto) {
        log.info("ProductDto, resource; update product with productId");
        return ResponseEntity.ok(productService.update(Long.parseLong(productId), productDto));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Boolean> deleteById(@PathVariable("productId") final String productId) {
        log.info("Boolean, resource; delete product by id");
        productService.deleteById(Long.parseLong(productId));
        return ResponseEntity.ok(true);
    }

}
