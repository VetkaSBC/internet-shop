package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.productservice.exception.wrapper.ProductNotFoundException;
import org.nicetu.spb.productservice.exception.wrapper.ProductPhotoNotFoundException;
import org.nicetu.spb.productservice.model.dto.ProductDto;
import org.nicetu.spb.productservice.model.dto.ProductPhotoDto;
import org.nicetu.spb.productservice.model.entity.Product;
import org.nicetu.spb.productservice.model.entity.ProductPhoto;
import org.nicetu.spb.productservice.repository.ProductPhotoRepository;
import org.nicetu.spb.productservice.repository.ProductRepository;
import org.nicetu.spb.productservice.service.impl.ProductPhotoServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ProductPhotoServiceImplTest {

    @Mock
    private ProductPhotoRepository productPhotoRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private ProductPhotoServiceImpl productPhotoService;

    private ProductPhotoDto testPhotoDto;
    private ProductPhoto testPhoto;
    private Product testProduct;

    @BeforeEach
    void setUp() throws IOException {
        testProduct = Product.builder()
                .productId(1L)
                .title("Test Product")
                .quantity(10)
                .priceUnit(100.0)
                .build();

        testPhoto = ProductPhoto.builder()
                .photoId(1L)
                .productId(1L)
                .photoLink("photo1.jpg")
                .originalFileName("photo.jpg")
                .fileSize(1024L)
                .contentType("image/jpeg")
                .uploadedAt(LocalDateTime.now())
                .product(testProduct)
                .build();

        testPhotoDto = ProductPhotoDto.builder()
                .photoId(1L)
                .photoLink("photo1.jpg")
                .productDto(ProductDto.builder()
                        .productId(1L)
                        .title("Test Product")
                        .build())
                .build();

        Path uploadPath = Paths.get("uploads");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
    }

    @Test
    void findById_shouldReturnPhotoWhenExists() {
        when(productPhotoRepository.findById(1L)).thenReturn(Mono.just(testPhoto));
        when(productRepository.findById(1L)).thenReturn(Mono.just(testProduct));

        Mono<ProductPhotoDto> result = productPhotoService.findById(1L);

        StepVerifier.create(result)
                .assertNext(photo -> {
                    assertThat(photo.getPhotoId()).isEqualTo(1L);
                    assertThat(photo.getPhotoLink()).isEqualTo("photo1.jpg");
                })
                .verifyComplete();

        verify(productPhotoRepository).findById(1L);
        verify(productRepository).findById(1L);
    }

    @Test
    void findById_shouldThrowExceptionWhenNotFound() {
        when(productPhotoRepository.findById(999L)).thenReturn(Mono.empty());

        Mono<ProductPhotoDto> result = productPhotoService.findById(999L);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof ProductPhotoNotFoundException &&
                                throwable.getMessage().contains("Photo not found"))
                .verify();

        verify(productPhotoRepository).findById(999L);
    }

    @Test
    void findAllPhotoByProductId_shouldReturnPhotos() {
        when(productPhotoRepository.findByProductId(1L)).thenReturn(Flux.just(testPhoto));
        when(productRepository.findById(1L)).thenReturn(Mono.just(testProduct));

        Flux<ProductPhotoDto> result = productPhotoService.findAllPhotoByProductId(1L);

        StepVerifier.create(result)
                .assertNext(photo -> {
                    assertThat(photo.getPhotoId()).isEqualTo(1L);
                    assertThat(photo.getPhotoLink()).isEqualTo("photo1.jpg");
                })
                .verifyComplete();

        verify(productPhotoRepository).findByProductId(1L);
        verify(productRepository).findById(1L);
    }

    @Test
    void createPhotoForProduct_shouldSavePhotoSuccessfully() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");

        when(productRepository.findById(1L)).thenReturn(Mono.just(testProduct));

        when(productPhotoRepository.save(any(ProductPhoto.class))).thenReturn(Mono.just(testPhoto));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });

        Mono<ProductPhotoDto> result = productPhotoService.createPhotoForProduct(1L, multipartFile);

        StepVerifier.create(result)
                .assertNext(photo -> {
                    assertThat(photo.getPhotoId()).isEqualTo(1L);
                    assertThat(photo.getPhotoLink()).isEqualTo("photo1.jpg");
                })
                .verifyComplete();

        verify(productRepository, times(2)).findById(1L);
        verify(productPhotoRepository).save(any(ProductPhoto.class));
    }

    @Test
    void createPhotoForProduct_shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Mono.empty());
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });

        Mono<ProductPhotoDto> result = productPhotoService.createPhotoForProduct(999L, multipartFile);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof RuntimeException) {
                        return throwable.getCause() instanceof ProductNotFoundException &&
                                throwable.getMessage().contains("Product not found with id: 999");
                    }
                    return throwable instanceof ProductNotFoundException &&
                            throwable.getMessage().contains("Product not found with id: 999");
                })
                .verify();

        verify(productRepository).findById(999L);
        verify(productPhotoRepository, never()).save(any(ProductPhoto.class));
    }

    @Test
    void getPhotoContent_shouldReturnPhotoBytes() throws IOException {
        when(productPhotoRepository.findById(1L)).thenReturn(Mono.just(testPhoto));

        Path testFile = Paths.get("uploads/photo1.jpg");
        Files.write(testFile, "test image content".getBytes());

        Mono<byte[]> result = productPhotoService.getPhotoContent(1L);

        StepVerifier.create(result)
                .assertNext(bytes -> {
                    assertThat(bytes).isNotEmpty();
                })
                .verifyComplete();

        Files.deleteIfExists(testFile);

        verify(productPhotoRepository).findById(1L);
    }

    @Test
    void getPhotoContentType_shouldReturnContentType() {
        when(productPhotoRepository.findById(1L)).thenReturn(Mono.just(testPhoto));

        Mono<String> result = productPhotoService.getPhotoContentType(1L);

        StepVerifier.create(result)
                .expectNext("image/jpeg")
                .verifyComplete();

        verify(productPhotoRepository).findById(1L);
    }

    @Test
    void getPhotoContentType_shouldReturnDefaultWhenNotFound() {
        ProductPhoto photoWithoutContentType = ProductPhoto.builder()
                .photoId(1L)
                .productId(1L)
                .photoLink("photo.jpg")
                .build();

        when(productPhotoRepository.findById(1L)).thenReturn(Mono.just(photoWithoutContentType));

        Mono<String> result = productPhotoService.getPhotoContentType(1L);

        StepVerifier.create(result)
                .expectNext(MediaType.IMAGE_JPEG_VALUE)
                .verifyComplete();

        verify(productPhotoRepository).findById(1L);
    }

    @Test
    void deletePhoto_shouldDeleteSuccessfully() {
        when(productPhotoRepository.findById(1L)).thenReturn(Mono.just(testPhoto));
        when(productPhotoRepository.delete(any(ProductPhoto.class))).thenReturn(Mono.empty());
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });

        Mono<Void> result = productPhotoService.deletePhoto(1L);

        StepVerifier.create(result)
                .verifyComplete();

        verify(productPhotoRepository).findById(1L);
        verify(productPhotoRepository).delete(any(ProductPhoto.class));
    }

    @Test
    void deletePhotosForProduct_shouldDeleteAllPhotos() {
        when(productPhotoRepository.findByProductId(1L)).thenReturn(Flux.just(testPhoto));
        when(productPhotoRepository.deleteByProductId(1L)).thenReturn(Mono.just(1));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });

        Mono<Void> result = productPhotoService.deletePhotosForProduct(1L);

        StepVerifier.create(result)
                .verifyComplete();

        verify(productPhotoRepository).findByProductId(1L);
        verify(productPhotoRepository).deleteByProductId(1L);
    }
}