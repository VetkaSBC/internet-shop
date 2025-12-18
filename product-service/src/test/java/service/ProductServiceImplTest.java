package service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.productservice.exception.wrapper.ProductNotFoundException;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.model.entity.Product;
import org.nicetu.spb.productservice.repository.CategoryRepository;
import org.nicetu.spb.productservice.repository.ProductCategoryRepository;
import org.nicetu.spb.productservice.repository.ProductRepository;
import org.nicetu.spb.productservice.service.impl.ProductServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    @Mock
    private ReactiveTransactionManager transactionManager;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductDto testProductDto;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProductDto = ProductDto.builder()
                .productId(1L)
                .title("Test Product")
                .description("Test Description")
                .quantity(10)
                .priceUnit(99.99)
                .discount(10L)
                .build();

        testProduct = Product.builder()
                .productId(1L)
                .title("Test Product")
                .description("Test Description")
                .quantity(10)
                .priceUnit(99.99)
                .discount(10L)
                .version(0)
                .build();
        testProduct.calculateQuantityStatus();
    }

    @Test
    void findById_shouldReturnProductWhenExists() {
        when(productRepository.findById(1L)).thenReturn(Mono.just(testProduct));
        when(categoryRepository.findByProductId(anyLong())).thenReturn(Flux.empty());

        Mono<ProductDto> result = productService.findById(1L);

        StepVerifier.create(result)
                .assertNext(product -> {
                    assertThat(product.getProductId()).isEqualTo(1L);
                    assertThat(product.getTitle()).isEqualTo("Test Product");
                    assertThat(product.getQuantity()).isEqualTo(10);
                })
                .verifyComplete();

        verify(productRepository).findById(1L);
    }

    @Test
    void findById_shouldThrowExceptionWhenNotFound() {
        when(productRepository.findById(999L)).thenReturn(Mono.empty());

        Mono<ProductDto> result = productService.findById(999L);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof ProductNotFoundException &&
                                throwable.getMessage().contains("Product not found"))
                .verify();

        verify(productRepository).findById(999L);
    }

    @Test
    void findAll_shouldReturnPagedProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Product> products = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(products, pageable, 1);

        when(productRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.fromIterable(products));
        when(productRepository.count()).thenReturn(Mono.just(1L));
        when(categoryRepository.findByProductId(anyLong())).thenReturn(Flux.empty());

        Mono<Page<ProductDto>> result = productService.findAll(0, 10);

        StepVerifier.create(result)
                .assertNext(page -> {
                    assertThat(page.getTotalElements()).isEqualTo(1);
                    assertThat(page.getContent()).hasSize(1);
                    assertThat(page.getContent().get(0).getTitle()).isEqualTo("Test Product");
                })
                .verifyComplete();

        verify(productRepository).findAllBy(any(Pageable.class));
        verify(productRepository).count();
    }

    @Test
    void save_shouldSaveProductSuccessfully() {
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(testProduct));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });
        when(categoryRepository.findByProductId(anyLong())).thenReturn(Flux.empty());

        Mono<ProductDto> result = productService.save(testProductDto);

        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertThat(saved.getProductId()).isEqualTo(1L);
                    assertThat(saved.getTitle()).isEqualTo("Test Product");
                })
                .verifyComplete();

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void update_shouldUpdateProductSuccessfully() {
        ProductDto updateDto = ProductDto.builder()
                .productId(1L)
                .title("Updated Title")
                .description("Updated Description")
                .quantity(20)
                .priceUnit(199.99)
                .discount(20L)
                .build();

        when(productRepository.findById(1L)).thenReturn(Mono.just(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(
                Product.builder()
                        .productId(1L)
                        .title("Updated Title")
                        .description("Updated Description")
                        .quantity(20)
                        .priceUnit(199.99)
                        .discount(20L)
                        .build()
        ));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });
        when(categoryRepository.findByProductId(anyLong())).thenReturn(Flux.empty());

        Mono<ProductDto> result = productService.update(updateDto);

        StepVerifier.create(result)
                .assertNext(updated -> {
                    assertThat(updated.getTitle()).isEqualTo("Updated Title");
                    assertThat(updated.getDescription()).isEqualTo("Updated Description");
                    assertThat(updated.getQuantity()).isEqualTo(20);
                })
                .verifyComplete();

        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void update_shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Mono.empty());
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });

        Mono<ProductDto> result = productService.update(ProductDto.builder()
                .productId(999L)
                .title("Not Found")
                .quantity(1)
                .priceUnit(100.0)
                .build());

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof ProductNotFoundException &&
                                throwable.getMessage().contains("Product not found"))
                .verify();

        verify(productRepository).findById(999L);
    }

    @Test
    void deleteById_shouldDeleteProduct() {
        when(productRepository.findById(1L)).thenReturn(Mono.just(testProduct));
        when(productCategoryRepository.deleteByProductId(1L)).thenReturn(Mono.just(1));
        when(productRepository.delete(any(Product.class))).thenReturn(Mono.empty());
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });

        Mono<Void> result = productService.deleteById(1L);

        StepVerifier.create(result)
                .verifyComplete();

        verify(productRepository).findById(1L);
        verify(productCategoryRepository).deleteByProductId(1L);
        verify(productRepository).delete(any(Product.class));
    }

    @Test
    void reserveProduct_shouldDecreaseQuantitySuccessfully() {
        Product reservedProduct = Product.builder()
                .productId(1L)
                .title("Test Product")
                .quantity(7)
                .priceUnit(99.99)
                .build();
        reservedProduct.calculateQuantityStatus();

        when(productRepository.reserveQuantity(1L, 3)).thenReturn(Mono.just(reservedProduct));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });
        when(categoryRepository.findByProductId(anyLong())).thenReturn(Flux.empty());

        Mono<ProductDto> result = productService.reserveProduct(1L, 3);

        StepVerifier.create(result)
                .assertNext(product -> {
                    assertThat(product.getQuantity()).isEqualTo(7);
                })
                .verifyComplete();

        verify(productRepository).reserveQuantity(1L, 3);
    }

    @Test
    void releaseProduct_shouldIncreaseQuantitySuccessfully() {
        Product releasedProduct = Product.builder()
                .productId(1L)
                .title("Test Product")
                .quantity(13)
                .priceUnit(99.99)
                .build();

        when(productRepository.incrementQuantity(1L, 3)).thenReturn(Mono.just(releasedProduct));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });
        when(categoryRepository.findByProductId(anyLong())).thenReturn(Flux.empty());

        Mono<ProductDto> result = productService.releaseProduct(1L, 3);

        StepVerifier.create(result)
                .assertNext(product -> {
                    assertThat(product.getQuantity()).isEqualTo(13);
                })
                .verifyComplete();

        verify(productRepository).incrementQuantity(1L, 3);
    }

    @Test
    void isProductAvailable_shouldReturnTrueWhenEnoughStock() {
        when(productRepository.findById(1L)).thenReturn(Mono.just(testProduct));

        Mono<Boolean> result = productService.isProductAvailable(1L, 5);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(productRepository).findById(1L);
    }

    @Test
    void isProductAvailable_shouldReturnFalseWhenNotEnoughStock() {
        when(productRepository.findById(1L)).thenReturn(Mono.just(testProduct));

        Mono<Boolean> result = productService.isProductAvailable(1L, 15);

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();

        verify(productRepository).findById(1L);
    }

    @Test
    void isProductAvailable_shouldReturnFalseWhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Mono.empty());

        Mono<Boolean> result = productService.isProductAvailable(999L, 5);

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();

        verify(productRepository).findById(999L);
    }
}