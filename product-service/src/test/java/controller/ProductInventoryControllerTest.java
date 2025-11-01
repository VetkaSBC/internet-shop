package controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.productservice.controller.ProductInventoryController;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ProductInventoryControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductInventoryController productInventoryController;

    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        productDto = ProductDto.builder()
                .productId(1L)
                .title("Smartphone")
                .quantity(10)
                .priceUnit(999.99)
                .build();
    }

    @Test
    void updateQuantity_ShouldReturnTrue() {
        when(productService.findById(1L)).thenReturn(productDto);
        when(productService.update(any(ProductDto.class))).thenReturn(productDto);

        ResponseEntity<Boolean> response = productInventoryController.updateQuantity(1L, 5);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
        verify(productService).findById(1L);
        verify(productService).update(any(ProductDto.class));
    }

    @Test
    void updateQuantity_WhenException_ShouldReturnFalse() {
        when(productService.findById(1L)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<Boolean> response = productInventoryController.updateQuantity(1L, 5);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody());
        verify(productService).findById(1L);
    }

    @Test
    void reserveProduct_WhenEnoughQuantity_ShouldReturnProduct() {
        when(productService.findById(1L)).thenReturn(productDto);
        when(productService.update(any(ProductDto.class))).thenReturn(productDto);

        ResponseEntity<ProductDto> response = productInventoryController.reserveProduct(1L, 5);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(productService).findById(1L);
        verify(productService).update(any(ProductDto.class));
    }

    @Test
    void reserveProduct_WhenNotEnoughQuantity_ShouldReturnBadRequest() {
        productDto.setQuantity(3);
        when(productService.findById(1L)).thenReturn(productDto);

        ResponseEntity<ProductDto> response = productInventoryController.reserveProduct(1L, 5);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(productService).findById(1L);
    }

    @Test
    void releaseProduct_ShouldReturnProduct() {
        when(productService.findById(1L)).thenReturn(productDto);
        when(productService.update(any(ProductDto.class))).thenReturn(productDto);

        ResponseEntity<ProductDto> response = productInventoryController.releaseProduct(1L, 5);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(productService).findById(1L);
        verify(productService).update(any(ProductDto.class));
    }

    @Test
    void checkAvailability_WhenAvailable_ShouldReturnAvailabilityInfo() {
        when(productService.findById(1L)).thenReturn(productDto);

        ResponseEntity<Map<String, Object>> response = productInventoryController.checkAvailability(1L, 5);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("isAvailable"));
        assertEquals(10, response.getBody().get("availableQuantity"));
        verify(productService).findById(1L);
    }

    @Test
    void checkAvailability_WhenNotAvailable_ShouldReturnAvailabilityInfo() {
        when(productService.findById(1L)).thenReturn(productDto);

        ResponseEntity<Map<String, Object>> response = productInventoryController.checkAvailability(1L, 15);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("isAvailable"));
        assertEquals(10, response.getBody().get("availableQuantity"));
        verify(productService).findById(1L);
    }
}