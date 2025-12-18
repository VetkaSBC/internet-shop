package org.nicetu.spb.mediaservice.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


import java.io.IOException;
import java.net.URI;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("api/media")
@Tag(name = "Photos")
@ApiResponses(@ApiResponse(responseCode = "200", useReturnTypeSchema = true))
public class PhotoController {

    @Value("${aws.access.key.id}")
    private String accessKeyId;

    @Value("${aws.secret.access.key}")
    private String secretAccessKey;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.endpoint}")
    private String s3Endpoint;

    @Value("${aws.s3.bucket}")
    private String bucket;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        s3Client = S3Client.builder()
                .httpClient(ApacheHttpClient.create())
                .region(Region.of(region))
                .endpointOverride(URI.create(s3Endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @PutMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    public String uploadPhoto(@RequestParam MultipartFile photo) throws IOException {

        String key = "photos/" + photo.getOriginalFilename();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(photo.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(photo.getBytes()));

        return key;
    }

    @GetMapping
    public ResponseEntity<byte[]> downloadPhoto(@RequestParam String key) throws IOException {

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        var inputStream = s3Client.getObject(objectRequest);
        byte[] data = inputStream.readAllBytes();

        var headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline");
        headers.add(HttpHeaders.CONTENT_TYPE, inputStream.response().contentType());

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}