package org.nicetu.spb.productservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.nicetu.spb.productservice.client.MediaServiceClient;
import org.nicetu.spb.productservice.exception.wrapper.ProductNotFoundException;
import org.nicetu.spb.productservice.exception.wrapper.ProductPhotoNotFoundException;
import org.nicetu.spb.productservice.mapper.ProductPhotoMapping;
import org.nicetu.spb.productservice.model.dto.ProductPhotoDto;
import org.nicetu.spb.productservice.model.entity.Product;
import org.nicetu.spb.productservice.model.entity.ProductPhoto;
import org.nicetu.spb.productservice.repository.ProductPhotoRepository;
import org.nicetu.spb.productservice.repository.ProductRepository;
import org.nicetu.spb.productservice.service.ProductPhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class ProductPhotoServiceImpl implements ProductPhotoService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPhotoRepository productPhotoRepository;

    @Autowired
    private MediaServiceClient mediaServiceClient;

    @Override
    public ProductPhotoDto findById(Long id) {
        ProductPhoto photo = productPhotoRepository.findById(id)
                .orElseThrow(() -> new ProductPhotoNotFoundException("Photo not found"));
        return ProductPhotoMapping.mapToDto(photo);
    }

    @Override
    public List<ProductPhotoDto> findAllPhotoByProductId(Long productId) {
        return productPhotoRepository.findByProductProductId(productId).stream()
                .map(ProductPhotoMapping::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductPhotoDto createPhoto(ProductPhotoDto photoDto) {
        log.info("ProductPhotoService: create new photo");
        try {
            Product product = productRepository.findById(photoDto.getProductDto().getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + photoDto.getProductDto().getProductId()));

            ProductPhoto photo = ProductPhoto.builder()
                    .photoLink(photoDto.getPhotoLink())
                    .product(product)
                    .build();

            ProductPhoto savedPhoto = productPhotoRepository.save(photo);
            return ProductPhotoMapping.mapToDto(savedPhoto);

        } catch (DataIntegrityViolationException e) {
            throw new ProductNotFoundException("Error saving photo: Data integrity violation", e);
        } catch (Exception e) {
            throw new ProductNotFoundException("Error saving photo: ", e);
        }
    }

    @Override
    public ProductPhotoDto createPhotoForProduct(Long productId, ProductPhotoDto photoDto) {
        log.info("ProductPhotoService: create photo for product id {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        ProductPhoto photo = ProductPhoto.builder()
                .photoLink(photoDto.getPhotoLink())
                .product(product)
                .build();

        ProductPhoto savedPhoto = productPhotoRepository.save(photo);
        return ProductPhotoMapping.mapToDto(savedPhoto);
    }

    @Override
    public ProductPhotoDto updatePhoto(Long id, ProductPhotoDto photoDto) {
        log.info("ProductPhotoService: update photo with id {}", id);

        ProductPhoto existingPhoto = productPhotoRepository.findById(id)
                .orElseThrow(() -> new ProductPhotoNotFoundException("Photo not found with id: " + id));

        existingPhoto.setPhotoLink(photoDto.getPhotoLink());

        if (photoDto.getProductDto().getProductId() != null &&
                !existingPhoto.getProduct().getProductId().equals(photoDto.getProductDto().getProductId())) {
            Product newProduct = productRepository.findById(photoDto.getProductDto().getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + photoDto.getProductDto().getProductId()));
            existingPhoto.setProduct(newProduct);
        }

        ProductPhoto updatedPhoto = productPhotoRepository.save(existingPhoto);
        return ProductPhotoMapping.mapToDto(updatedPhoto);
    }

    @Override
    public void deletePhoto(Long id) {
        log.info("ProductPhotoService: delete photo with id {}", id);

        if (!productPhotoRepository.existsById(id)) {
            throw new ProductPhotoNotFoundException("Photo not found with id: " + id);
        }

        productPhotoRepository.deleteById(id);
    }

    @Override
    public void deletePhotosForProduct(Long productId) {
        log.info("ProductPhotoService: delete all photos for product id {}", productId);

        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException("Product not found with id: " + productId);
        }

        productPhotoRepository.deleteByProductId(productId);
    }

    @Override
    public byte[] getPhotoContent(Long photoId) {
        log.info("Getting photo content for ID: {}", photoId);

        ProductPhoto photo = productPhotoRepository.findById(photoId)
                .orElseThrow(() -> new ProductPhotoNotFoundException("Photo not found with id: " + photoId));

        try {
            byte[] content = mediaServiceClient.downloadPhoto(photo.getPhotoLink());
            log.info("Successfully downloaded photo content for ID: {}", photoId);
            return content;

        } catch (Exception e) {
            log.error("Failed to download photo content for ID {}: {}", photoId, e.getMessage());
            throw new RuntimeException("Failed to download photo: " + e.getMessage(), e);
        }
    }

    @Override
    public ProductPhotoDto createPhotoForProduct(Long productId, MultipartFile photoFile) {
        log.info("Creating photo for product ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        try {
            String photoKey = mediaServiceClient.uploadPhoto(photoFile);
            log.info("Photo uploaded successfully. Key: {}", photoKey);

            ProductPhoto productPhoto = ProductPhoto.builder()
                    .photoLink(photoKey)
                    .product(product)
                    .originalFileName(photoFile.getOriginalFilename())
                    .fileSize(photoFile.getSize())
                    .contentType(photoFile.getContentType())
                    .uploadedAt(LocalDateTime.now())
                    .build();

            ProductPhoto savedPhoto = productPhotoRepository.save(productPhoto);
            log.info("Photo saved to database with ID: {}", savedPhoto.getPhotoId());

            return ProductPhotoMapping.mapToDto(savedPhoto);

        } catch (Exception e) {
            log.error("Failed to create photo for product {}: {}", productId, e.getMessage());
            throw new RuntimeException("Failed to upload photo: " + e.getMessage(), e);
        }
    }
}
