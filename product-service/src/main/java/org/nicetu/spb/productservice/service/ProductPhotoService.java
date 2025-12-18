package org.nicetu.spb.productservice.service;

import org.nicetu.spb.productservice.model.dto.ProductPhotoDto;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductPhotoService {

    Mono<ProductPhotoDto> findById(Long id);
    Flux<ProductPhotoDto> findAllPhotoByProductId(Long productId);
    Mono<byte[]> getPhotoContent(Long photoId);
    Mono<String> getPhotoContentType(Long photoId);
    Mono<ProductPhotoDto> createPhotoForProduct(Long productId, MultipartFile photoFile);
    Mono<ProductPhotoDto> createPhoto(ProductPhotoDto productPhotoDto);
    Mono<ProductPhotoDto> createPhotoForProduct(Long productId, ProductPhotoDto productPhotoDto);
    Mono<ProductPhotoDto> updatePhoto(Long id, ProductPhotoDto productPhotoDto);
    Mono<Void> deletePhoto(Long id);
    Mono<Void> deletePhotosForProduct(Long productId);
}