package controller;

import org.junit.jupiter.api.Test;
import org.nicetu.spb.productservice.controller.ProductController;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest
@ContextConfiguration(classes = ProductControllerTest.TestConfig.class)
class ProductControllerTest {

    @Configuration
    static class TestConfig {
        @Bean
        public ProductController productController(ProductService productService) {
            return new ProductController(productService);
        }

        @Bean
        public ProductService productService() {
            return mock(ProductService.class);
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ProductService productService;

    @Test
    void getProductById_shouldReturnProduct() {
        ProductDto productDto = ProductDto.builder()
                .productId(1L)
                .title("Test Product")
                .quantity(10)
                .priceUnit(99.99)
                .build();

        when(productService.findById(1L)).thenReturn(Mono.just(productDto));

        webTestClient.get()
                .uri("/api/products/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.productId").isEqualTo(1)
                .jsonPath("$.title").isEqualTo("Test Product")
                .jsonPath("$.quantity").isEqualTo(10);
    }

    @Test
    void getProductById_shouldReturnNotFound() {
        when(productService.findById(999L)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/products/{id}", 999)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getAllProducts_shouldReturnProducts() {
        ProductDto productDto = ProductDto.builder()
                .productId(1L)
                .title("Test Product")
                .quantity(10)
                .priceUnit(99.99)
                .build();

        Page<ProductDto> page = new PageImpl<>(List.of(productDto), PageRequest.of(0, 10), 1);
        when(productService.findAll(anyInt(), anyInt())).thenReturn(Mono.just(page));

        webTestClient.get()
                .uri("/api/products?page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].productId").isEqualTo(1)
                .jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void createProduct_shouldReturnCreated() {
        ProductDto request = ProductDto.builder()
                .title("New Product")
                .quantity(5)
                .priceUnit(50.0)
                .build();

        ProductDto response = ProductDto.builder()
                .productId(1L)
                .title("New Product")
                .quantity(5)
                .priceUnit(50.0)
                .build();

        when(productService.save(any(ProductDto.class))).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.productId").isEqualTo(1)
                .jsonPath("$.title").isEqualTo("New Product");
    }

    @Test
    void updateProduct_shouldReturnUpdatedProduct() {
        ProductDto request = ProductDto.builder()
                .productId(1L)
                .title("Updated Product")
                .quantity(15)
                .priceUnit(75.0)
                .build();

        when(productService.update(any(ProductDto.class))).thenReturn(Mono.just(request));

        webTestClient.put()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.productId").isEqualTo(1)
                .jsonPath("$.title").isEqualTo("Updated Product")
                .jsonPath("$.quantity").isEqualTo(15);
    }

    @Test
    void deleteProduct_shouldReturnNoContent() {
        when(productService.deleteById(1L)).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/products/{id}", 1)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void reserveProduct_shouldDecreaseQuantity() {
        ProductDto response = ProductDto.builder()
                .productId(1L)
                .title("Product")
                .quantity(7)
                .priceUnit(100.0)
                .build();

        when(productService.reserveProduct(1L, 3)).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/products/{id}/reserve?quantity=3", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.quantity").isEqualTo(7);
    }

    @Test
    void releaseProduct_shouldIncreaseQuantity() {
        ProductDto response = ProductDto.builder()
                .productId(1L)
                .title("Product")
                .quantity(13)
                .priceUnit(100.0)
                .build();

        when(productService.releaseProduct(1L, 3)).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/products/{id}/release?quantity=3", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.quantity").isEqualTo(13);
    }

    @Test
    void checkAvailability_shouldReturnBoolean() {
        when(productService.isProductAvailable(1L, 5)).thenReturn(Mono.just(true));
        when(productService.isProductAvailable(1L, 15)).thenReturn(Mono.just(false));

        webTestClient.get()
                .uri("/api/products/{id}/available?quantity=5", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class)
                .isEqualTo(true);

        webTestClient.get()
                .uri("/api/products/{id}/available?quantity=15", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class)
                .isEqualTo(false);
    }

    @Test
    void updateProduct_withInvalidId_shouldReturnNotFound() {
        ProductDto request = ProductDto.builder()
                .productId(999L)
                .title("Not Found Product")
                .quantity(10)
                .priceUnit(100.0)
                .build();

        when(productService.update(any(ProductDto.class))).thenReturn(Mono.empty());

        webTestClient.put()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }
}