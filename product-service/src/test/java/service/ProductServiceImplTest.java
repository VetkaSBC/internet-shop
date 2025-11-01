package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.productservice.exception.wrapper.ProductNotFoundException;
import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.model.entity.Category;
import org.nicetu.spb.productservice.model.entity.Product;
import org.nicetu.spb.productservice.repository.ProductRepository;
import org.nicetu.spb.productservice.service.impl.ProductServiceImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductDto productDto;
    private Category category;
    private CategoryDto categoryDto;


    @BeforeEach
    void setUp() {
        category = Category.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();

        categoryDto = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();

        product = Product.builder()
                .productId(1L)
                .title("Smartphone")
                .description("Latest smartphone")
                .quantity(10)
                .priceUnit(999.99)
                .discount(0L)
                .category(category)
                .build();

        productDto = ProductDto.builder()
                .productId(1L)
                .title("Smartphone")
                .description("Latest smartphone")
                .quantity(10)
                .priceUnit(999.99)
                .discount(0L)
                .categoryDto(categoryDto)
                .build();
    }

        @Test
    void findAll_ShouldReturnFluxOfProductDtos() {
        List<Product> products = Arrays.asList(product);
        when(productRepository.findAll()).thenReturn(products);

        var resultFlux = productService.findAll();
        List<List<ProductDto>> result = resultFlux.collectList().block();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(productRepository).findAll();
    }

    @Test
    void findById_WhenProductExists_ShouldReturnProductDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDto result = productService.findById(1L);

        assertNotNull(result);
        verify(productRepository).findById(1L);
    }

    @Test
    void findById_WhenProductNotExists_ShouldThrowException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.findById(1L));
        verify(productRepository).findById(1L);
    }

    @Test
    void save_ShouldReturnProductDto() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDto result = productService.save(productDto);

        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void update_WhenProductExists_ShouldReturnUpdatedProductDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDto result = productService.update(productDto);

        assertNotNull(result);
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void update_WhenProductNotExists_ShouldThrowException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.update(productDto));
        verify(productRepository).findById(1L);
    }

    @Test
    void updateWithId_WhenProductExists_ShouldReturnUpdatedProductDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDto result = productService.update(1L, productDto);

        assertNotNull(result);
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void deleteById_ShouldCallRepositoryDelete() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).delete(any(Product.class));

        productService.deleteById(1L);

        verify(productRepository).delete(any(Product.class));
    }

    @Test
    void isProductAvailable_WhenEnoughQuantity_ShouldReturnTrue() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        boolean result = productService.isProductAvailable(1L, 5);

        assertTrue(result);
    }

    @Test
    void isProductAvailable_WhenNotEnoughQuantity_ShouldReturnFalse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        boolean result = productService.isProductAvailable(1L, 15);

        assertFalse(result);
    }

    @Test
    void reserveProduct_WhenEnoughQuantity_ShouldReturnUpdatedProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDto result = productService.reserveProduct(1L, 5);

        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void reserveProduct_WhenNotEnoughQuantity_ShouldThrowException() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(ProductNotFoundException.class, () -> productService.reserveProduct(1L, 15));
    }

    @Test
    void releaseProduct_ShouldReturnUpdatedProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDto result = productService.releaseProduct(1L, 5);

        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
    }
}