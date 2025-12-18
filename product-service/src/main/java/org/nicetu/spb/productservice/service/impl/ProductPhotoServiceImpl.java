package org.nicetu.spb.productservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.productservice.exception.wrapper.ProductNotFoundException;
import org.nicetu.spb.productservice.exception.wrapper.ProductPhotoNotFoundException;
import org.nicetu.spb.productservice.mapper.ProductPhotoMapping;
import org.nicetu.spb.productservice.model.dto.ProductPhotoDto;
import org.nicetu.spb.productservice.model.entity.ProductPhoto;
import org.nicetu.spb.productservice.repository.ProductPhotoRepository;
import org.nicetu.spb.productservice.repository.ProductRepository;
import org.nicetu.spb.productservice.service.ProductPhotoService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductPhotoServiceImpl implements ProductPhotoService {

    private final ProductPhotoRepository productPhotoRepository;
    private final ProductRepository productRepository;
    private final TransactionalOperator transactionalOperator;

    private final Path uploadPath = Paths.get("uploads");

    @Override
    public Mono<ProductPhotoDto> findById(Long id) {
        log.info("Fetching photo by id: {}", id);

        return productPhotoRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductPhotoNotFoundException(
                        "Photo not found with id: " + id
                )))
                .flatMap(this::enrichPhotoWithProduct)
                .map(ProductPhotoMapping::mapToDto);
    }

    @Override
    public Flux<ProductPhotoDto> findAllPhotoByProductId(Long productId) {
        log.info("Fetching photos for product: {}", productId);

        return productPhotoRepository.findByProductId(productId)
                .flatMap(this::enrichPhotoWithProduct)
                .map(ProductPhotoMapping::mapToDto);
    }

    @Override
    public Mono<ProductPhotoDto> createPhotoForProduct(Long productId, MultipartFile photoFile) {
        log.info("Creating photo for product: {}", productId);

        return transactionalOperator.transactional(
                Mono.defer(() -> {
                    return productRepository.findById(productId)
                            .switchIfEmpty(Mono.error(new ProductNotFoundException(
                                    "Product not found with id: " + productId
                            )))
                            .flatMap(product -> savePhotoFileReactive(photoFile, productId)
                                    .flatMap(photoLink -> {
                                        ProductPhoto photo = ProductPhoto.builder()
                                                .productId(productId)
                                                .photoLink(photoLink)
                                                .originalFileName(photoFile.getOriginalFilename())
                                                .fileSize(photoFile.getSize())
                                                .contentType(photoFile.getContentType())
                                                .uploadedAt(LocalDateTime.now())
                                                .build();

                                        return productPhotoRepository.save(photo);
                                    })
                            )
                            .flatMap(this::enrichPhotoWithProduct)
                            .map(ProductPhotoMapping::mapToDto);
                })
        ).onErrorResume(e -> {
            log.error("Error creating photo: {}", e.getMessage());
            return Mono.error(new RuntimeException("Error creating photo: " + e.getMessage(), e));
        });
    }

    @Override
    public Mono<byte[]> getPhotoContent(Long photoId) {
        log.info("Getting photo content: {}", photoId);

        return productPhotoRepository.findById(photoId)
                .switchIfEmpty(Mono.error(new ProductPhotoNotFoundException(
                        "Photo not found with id: " + photoId
                )))
                .flatMap(photo -> readFileContentReactive(photo.getPhotoLink()));
    }

    @Override
    public Mono<String> getPhotoContentType(Long photoId) {
        return productPhotoRepository.findById(photoId)
                .map(photo -> photo.getContentType() != null ?
                        photo.getContentType() : MediaType.IMAGE_JPEG_VALUE)
                .defaultIfEmpty(MediaType.IMAGE_JPEG_VALUE);
    }

    @Override
    public Mono<ProductPhotoDto> createPhoto(ProductPhotoDto productPhotoDto) {
        log.info("Creating photo from DTO");

        return transactionalOperator.transactional(
                Mono.defer(() -> {
                    Long productId = productPhotoDto.getProductDto() != null ?
                            productPhotoDto.getProductDto().getProductId() : null;

                    if (productId == null) {
                        return Mono.error(new IllegalArgumentException("Product ID is required"));
                    }

                    return productRepository.findById(productId)
                            .switchIfEmpty(Mono.error(new ProductNotFoundException(
                                    "Product not found with id: " + productId
                            )))
                            .flatMap(product -> {
                                ProductPhoto photo = ProductPhotoMapping.mapToEntity(productPhotoDto);
                                photo.setProductId(productId);

                                return productPhotoRepository.save(photo);
                            })
                            .flatMap(this::enrichPhotoWithProduct)
                            .map(ProductPhotoMapping::mapToDto);
                })
        );
    }

    @Override
    public Mono<ProductPhotoDto> createPhotoForProduct(Long productId, ProductPhotoDto productPhotoDto) {
        log.info("Creating photo for product {} from DTO", productId);

        productPhotoDto.getProductDto().setProductId(productId);
        return createPhoto(productPhotoDto);
    }

    @Override
    public Mono<ProductPhotoDto> updatePhoto(Long id, ProductPhotoDto productPhotoDto) {
        log.info("Updating photo: {}", id);

        return transactionalOperator.transactional(
                Mono.defer(() -> {
                    return productPhotoRepository.findById(id)
                            .switchIfEmpty(Mono.error(new ProductPhotoNotFoundException(
                                    "Photo not found with id: " + id
                            )))
                            .flatMap(existingPhoto -> {
                                existingPhoto.setPhotoLink(productPhotoDto.getPhotoLink());

                                if (productPhotoDto.getProductDto() != null &&
                                        productPhotoDto.getProductDto().getProductId() != null) {
                                    existingPhoto.setProductId(productPhotoDto.getProductDto().getProductId());
                                }

                                return productPhotoRepository.save(existingPhoto);
                            })
                            .flatMap(this::enrichPhotoWithProduct)
                            .map(ProductPhotoMapping::mapToDto);
                })
        );
    }

    @Override
    public Mono<Void> deletePhoto(Long id) {
        log.info("Deleting photo: {}", id);

        return transactionalOperator.transactional(
                Mono.defer(() -> {
                    return productPhotoRepository.findById(id)
                            .switchIfEmpty(Mono.error(new ProductPhotoNotFoundException(
                                    "Photo not found with id: " + id
                            )))
                            .flatMap(photo -> {
                                return deletePhotoFile(photo.getPhotoLink())
                                        .then(productPhotoRepository.delete(photo));
                            });
                })
        ).then();
    }

    @Override
    public Mono<Void> deletePhotosForProduct(Long productId) {
        log.info("Deleting all photos for product: {}", productId);

        return transactionalOperator.transactional(
                Mono.defer(() -> {
                    return productPhotoRepository.findByProductId(productId)
                            .collectList()
                            .flatMap(photos -> {
                                return Flux.fromIterable(photos)
                                        .flatMap(photo -> deletePhotoFile(photo.getPhotoLink()))
                                        .then(Mono.just(photos));
                            })
                            .flatMap(photos -> productPhotoRepository.deleteByProductId(productId));
                })
        ).then();
    }


    private Mono<String> savePhotoFileReactive(MultipartFile file, Long productId) {
        return Mono.fromCallable(() -> {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String fileName = "product_" + productId + "_" +
                    UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(fileName);

            file.transferTo(filePath.toFile());

            return fileName;
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private Mono<byte[]> readFileContentReactive(String fileName) {
        return Mono.fromCallable(() -> {
            Path filePath = uploadPath.resolve(fileName);
            if (!Files.exists(filePath)) {
                throw new ProductPhotoNotFoundException("Photo file not found: " + fileName);
            }
            return Files.readAllBytes(filePath);
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private Mono<Void> deletePhotoFile(String fileName) {
        return Mono.fromCallable(() -> {
            Path filePath = uploadPath.resolve(fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            return null;
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).then();
    }

    private Mono<ProductPhoto> enrichPhotoWithProduct(ProductPhoto photo) {
        return productRepository.findById(photo.getProductId())
                .doOnNext(photo::setProduct)
                .thenReturn(photo);
    }

    private Mono<String> savePhotoFileWithDataBuffer(FilePart filePart, Long productId) {
        String fileName = "product_" + productId + "_" +
                UUID.randomUUID().toString() + "_" +
                filePart.filename();

        Path filePath = uploadPath.resolve(fileName);

        return filePart.content()
                .collectList()
                .flatMap(dataBuffers -> {
                    return Mono.fromCallable(() -> {
                        if (!Files.exists(uploadPath)) {
                            Files.createDirectories(uploadPath);
                        }

                        try (var channel = Files.newByteChannel(filePath,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE)) {

                            for (DataBuffer buffer : dataBuffers) {
                                byte[] bytes = new byte[buffer.readableByteCount()];
                                buffer.read(bytes);
                                channel.write(java.nio.ByteBuffer.wrap(bytes));
                                DataBufferUtils.release(buffer);
                            }
                        }
                        return fileName;
                    }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
                });
    }
}