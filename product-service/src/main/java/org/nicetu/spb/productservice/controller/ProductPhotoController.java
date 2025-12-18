package org.nicetu.spb.productservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.productservice.model.dto.ProductPhotoDto;
import org.nicetu.spb.productservice.service.ProductPhotoService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/{productId}/photos")
public class ProductPhotoController {

    private final ProductPhotoService productPhotoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<ProductPhotoDto>> uploadPhoto(
            @PathVariable Long productId,
            @RequestParam("photo") MultipartFile photo) {

        log.info("Uploading photo for product: {}", productId);

        return productPhotoService.createPhotoForProduct(productId, photo)
                .map(photoDto -> ResponseEntity.status(HttpStatus.CREATED).body(photoDto))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping("/{photoId}/content")
    public Mono<ResponseEntity<Flux<DataBuffer>>> getPhotoContent(
            @PathVariable Long productId,
            @PathVariable Long photoId) {

        log.info("Getting photo content: productId={}, photoId={}", productId, photoId);

        return productPhotoService.getPhotoContent(photoId)
                .flatMap(bytes -> productPhotoService.getPhotoContentType(photoId)
                        .map(contentType -> {
                            DataBuffer buffer = new DefaultDataBufferFactory().wrap(bytes);
                            return ResponseEntity.ok()
                                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                                    .body(Flux.just(buffer));
                        }))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Mono<ResponseEntity<List<ProductPhotoDto>>> getProductPhotos(
            @PathVariable Long productId) {

        log.info("Getting photos for product: {}", productId);

        return productPhotoService.findAllPhotoByProductId(productId)
                .collectList()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.ok(List.of()));
    }

    @GetMapping("/{photoId}")
    public Mono<ResponseEntity<ProductPhotoDto>> getPhoto(
            @PathVariable Long productId,
            @PathVariable Long photoId) {

        log.info("Getting photo: productId={}, photoId={}", productId, photoId);

        return productPhotoService.findById(photoId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}