package controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.productservice.controller.ProductController;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        productDto = ProductDto.builder()
                .productId(1L)
                .title("Smartphone")
                .description("Latest smartphone")
                .quantity(10)
                .priceUnit(999.99)
                .build();
    }

    @Test
    void findAll_ShouldReturnFluxOfProducts() {
        List<ProductDto> products = Arrays.asList(productDto);
        when(productService.findAll()).thenReturn(Flux.just(products));

        Flux<List<ProductDto>> result = productController.findAll();

        assertNotNull(result);
        verify(productService).findAll();
    }

    @Test
    void findById_ShouldReturnProduct() {
        when(productService.findById(1L)).thenReturn(productDto);

        ResponseEntity<ProductDto> response = productController.findById("1");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(productService).findById(1L);
    }

    @Test
    void save_ShouldReturnSavedProduct() {
        when(productService.save(any(ProductDto.class))).thenReturn(productDto);

        ResponseEntity<ProductDto> response = productController.save(productDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(productService).save(any(ProductDto.class));
    }

    @Test
    void update_ShouldReturnUpdatedProduct() {
        when(productService.update(any(ProductDto.class))).thenReturn(productDto);

        ResponseEntity<ProductDto> response = productController.update(productDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(productService).update(any(ProductDto.class));
    }

    @Test
    void updateWithId_ShouldReturnUpdatedProduct() {
        when(productService.update(anyLong(), any(ProductDto.class))).thenReturn(productDto);

        ResponseEntity<ProductDto> response = productController.update("1", productDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(productService).update(1L, productDto);
    }

    @Test
    void deleteById_ShouldReturnTrue() {
        doNothing().when(productService).deleteById(1L);

        ResponseEntity<Boolean> response = productController.deleteById("1");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
        verify(productService).deleteById(1L);
    }
}