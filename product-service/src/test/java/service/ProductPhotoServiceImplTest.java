package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.productservice.client.MediaServiceClient;
import org.nicetu.spb.productservice.exception.wrapper.ProductPhotoNotFoundException;
import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.model.dto.ProductPhotoDto;
import org.nicetu.spb.productservice.model.entity.Category;
import org.nicetu.spb.productservice.model.entity.Product;
import org.nicetu.spb.productservice.model.entity.ProductPhoto;
import org.nicetu.spb.productservice.repository.ProductPhotoRepository;
import org.nicetu.spb.productservice.repository.ProductRepository;
import org.nicetu.spb.productservice.service.impl.ProductPhotoServiceImpl;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class ProductPhotoServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductPhotoRepository productPhotoRepository;

    @Mock
    private MediaServiceClient mediaServiceClient;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private ProductPhotoServiceImpl productPhotoService;

    private Product product;
    private ProductPhoto productPhoto;
    private ProductPhotoDto productPhotoDto;
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
                .category(category)
                .build();

        productDto = ProductDto.builder()
                .productId(1L)
                .title("Smartphone")
                .categoryDto(categoryDto)
                .build();

        productPhoto = ProductPhoto.builder()
                .photoId(1L)
                .photoLink("photo-key")
                .product(product)
                .build();

        productPhotoDto = ProductPhotoDto.builder()
                .photoId(1L)
                .photoLink("photo-key")
                .productDto(productDto)
                .build();
    }

    @Test
    void findById_WhenPhotoExists_ShouldReturnProductPhotoDto() {
        when(productPhotoRepository.findById(1L)).thenReturn(Optional.of(productPhoto));

        ProductPhotoDto result = productPhotoService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPhotoId());
        assertEquals("photo-key", result.getPhotoLink());
        verify(productPhotoRepository).findById(1L);
    }


    @Test
    void findById_WhenPhotoNotExists_ShouldThrowException() {
        when(productPhotoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductPhotoNotFoundException.class, () -> productPhotoService.findById(1L));
    }

    @Test
    void findAllPhotoByProductId_ShouldReturnListOfProductPhotoDtos() {
        when(productPhotoRepository.findByProductProductId(1L)).thenReturn(Arrays.asList(productPhoto));

        List<ProductPhotoDto> result = productPhotoService.findAllPhotoByProductId(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(productPhotoRepository).findByProductProductId(1L);
    }

    @Test
    void getPhotoContent_ShouldReturnPhotoBytes() {
        byte[] photoBytes = new byte[]{1, 2, 3};
        when(productPhotoRepository.findById(1L)).thenReturn(Optional.of(productPhoto));
        when(mediaServiceClient.downloadPhoto("photo-key")).thenReturn(photoBytes);

        byte[] result = productPhotoService.getPhotoContent(1L);

        assertNotNull(result);
        assertEquals(photoBytes, result);
        verify(mediaServiceClient).downloadPhoto("photo-key");
    }

    @Test
    void createPhoto_ShouldReturnProductPhotoDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productPhotoRepository.save(any(ProductPhoto.class))).thenReturn(productPhoto);

        ProductPhotoDto result = productPhotoService.createPhoto(productPhotoDto);

        assertNotNull(result);
        verify(productPhotoRepository).save(any(ProductPhoto.class));
    }

    @Test
    void createPhotoForProduct_ShouldReturnProductPhotoDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productPhotoRepository.save(any(ProductPhoto.class))).thenReturn(productPhoto);

        ProductPhotoDto result = productPhotoService.createPhotoForProduct(1L, productPhotoDto);

        assertNotNull(result);
        verify(productPhotoRepository).save(any(ProductPhoto.class));
    }

    @Test
    void createPhotoForProductWithFile_ShouldReturnProductPhotoDto() throws IOException {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(mediaServiceClient.uploadPhoto(multipartFile)).thenReturn("new-photo-key");
        when(productPhotoRepository.save(any(ProductPhoto.class))).thenReturn(productPhoto);

        ProductPhotoDto result = productPhotoService.createPhotoForProduct(1L, multipartFile);

        assertNotNull(result);
        verify(mediaServiceClient).uploadPhoto(multipartFile);
        verify(productPhotoRepository).save(any(ProductPhoto.class));
    }

    @Test
    void updatePhoto_WhenPhotoExists_ShouldReturnUpdatedProductPhotoDto() {
        when(productPhotoRepository.findById(1L)).thenReturn(Optional.of(productPhoto));
        when(productPhotoRepository.save(any(ProductPhoto.class))).thenReturn(productPhoto);

        ProductPhotoDto result = productPhotoService.updatePhoto(1L, productPhotoDto);

        assertNotNull(result);
        verify(productPhotoRepository).save(any(ProductPhoto.class));
    }

    @Test
    void deletePhoto_WhenPhotoExists_ShouldCallRepositoryDelete() {
        when(productPhotoRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productPhotoRepository).deleteById(1L);

        productPhotoService.deletePhoto(1L);

        verify(productPhotoRepository).deleteById(1L);
    }

    @Test
    void deletePhoto_WhenPhotoNotExists_ShouldThrowException() {
        when(productPhotoRepository.existsById(1L)).thenReturn(false);

        assertThrows(ProductPhotoNotFoundException.class, () -> productPhotoService.deletePhoto(1L));
    }
}