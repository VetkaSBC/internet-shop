package org.nicetu.spb.productservice.controller;

import lombok.RequiredArgsConstructor;


import org.nicetu.spb.productservice.model.dto.ProductPhotoDto;
import org.nicetu.spb.productservice.service.ProductPhotoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/photos")
@RequiredArgsConstructor
public class ProductPhotoController {

    private final ProductPhotoService productPhotoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductPhotoDto> uploadPhoto(
            @PathVariable Long productId,
            @RequestParam("photo") MultipartFile photo) {

        ProductPhotoDto photoDto = productPhotoService.createPhotoForProduct(productId, photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(photoDto);
    }

    @GetMapping("/{photoId}/content")
    public ResponseEntity<byte[]> getPhotoContent(
            @PathVariable Long productId,
            @PathVariable Long photoId) {

        byte[] content = productPhotoService.getPhotoContent(photoId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .body(content);
    }

    @GetMapping
    public ResponseEntity<List<ProductPhotoDto>> getProductPhotos(@PathVariable Long productId) {
        List<ProductPhotoDto> photos = productPhotoService.findAllPhotoByProductId(productId);
        return ResponseEntity.ok(photos);
    }

    @GetMapping("/{photoId}")
    public ResponseEntity<ProductPhotoDto> getPhoto(@PathVariable Long productId,
                                                   @PathVariable Long photoId) {
        ProductPhotoDto photo = productPhotoService.findById(photoId);
        return ResponseEntity.ok(photo);
    }




}