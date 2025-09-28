package org.nicetu.spb.productservice.service;

import org.nicetu.spb.productservice.model.dto.ProductPhotoDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductPhotoService {

    ProductPhotoDto findById(Long id);
    List<ProductPhotoDto> findAllPhotoByProductId(Long productId);
    byte[] getPhotoContent(Long photoId);
    ProductPhotoDto createPhoto(ProductPhotoDto productPhotoDto);
    ProductPhotoDto createPhotoForProduct(Long routeId, ProductPhotoDto productPhotoDto);
    ProductPhotoDto createPhotoForProduct(Long routeId, MultipartFile photoFile);
    ProductPhotoDto updatePhoto(Long id, ProductPhotoDto productPhotoDto);
    void deletePhoto(Long id);
    void deletePhotosForProduct(Long productId);
}
