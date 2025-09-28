package org.nicetu.spb.productservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceClient {

    private final WebClient mediaServiceWebClient;

    public String uploadPhoto(MultipartFile photo) throws IOException {
        try {
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("photo", new ByteArrayResource(photo.getBytes()) {
                @Override
                public String getFilename() {
                    return photo.getOriginalFilename();
                }
            }).contentType(MediaType.parseMediaType(photo.getContentType()));

            return mediaServiceWebClient.put()
                    .uri("/api/media")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

        } catch (WebClientResponseException e) {
            log.error("MediaService error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to upload photo to MediaService: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during photo upload: {}", e.getMessage());
            throw new RuntimeException("Photo upload failed: " + e.getMessage());
        }
    }

    public byte[] downloadPhoto(String photoKey) {
        try {
            return mediaServiceWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/media")
                            .queryParam("key", photoKey)
                            .build())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

        } catch (WebClientResponseException e) {
            log.error("MediaService error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to download photo from MediaService: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during photo download: {}", e.getMessage());
            throw new RuntimeException("Photo download failed: " + e.getMessage());
        }
    }
}
