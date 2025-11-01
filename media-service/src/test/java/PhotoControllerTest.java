import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.mediaservice.controller.PhotoController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoControllerTest {

    private PhotoController photoController;

    @Mock
    private S3Client mockS3Client;

    private MultipartFile mockMultipartFile;

    @BeforeEach
    void setUp() throws Exception {
        photoController = new PhotoController();

        Field s3ClientField = PhotoController.class.getDeclaredField("s3Client");
        s3ClientField.setAccessible(true);
        s3ClientField.set(photoController, mockS3Client);

        mockMultipartFile = new MockMultipartFile(
                "photo",
                "test-photo.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
    }

    @Test
    void testUploadPhoto_Success() throws IOException {
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String result = photoController.uploadPhoto(mockMultipartFile);

        assertNotNull(result);
        assertEquals("photos/test-photo.jpg", result);

        verify(mockS3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }



    @Test
    void testUploadPhoto_WithNullFilename() throws IOException {
        MultipartFile fileWithNullName = mock(MultipartFile.class);
        when(fileWithNullName.getOriginalFilename()).thenReturn(null);
        when(fileWithNullName.getContentType()).thenReturn("image/jpeg");
        when(fileWithNullName.getBytes()).thenReturn("test content".getBytes());

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String result = photoController.uploadPhoto(fileWithNullName);

        assertNotNull(result);
        assertEquals("photos/null", result);
        verify(mockS3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

        @Test
    void testUploadPhoto_WithEmptyFilename() throws IOException {
        MultipartFile fileWithEmptyName = new MockMultipartFile(
                "photo",
                "",
                "image/jpeg",
                "test content".getBytes()
        );

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String result = photoController.uploadPhoto(fileWithEmptyName);

        assertNotNull(result);
        assertEquals("photos/", result);
        verify(mockS3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadPhoto_IOException() throws IOException {
        MultipartFile corruptFile = mock(MultipartFile.class);
        when(corruptFile.getOriginalFilename()).thenReturn("test.jpg");
        when(corruptFile.getContentType()).thenReturn("image/jpeg");
        when(corruptFile.getBytes()).thenThrow(new IOException("File read error"));

        assertThrows(IOException.class, () -> photoController.uploadPhoto(corruptFile));

        verify(mockS3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadPhoto_NullContentType() throws IOException {
        MultipartFile fileWithNullContentType = new MockMultipartFile(
                "photo",
                "test.jpg",
                null,
                "test content".getBytes()
        );

        when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String result = photoController.uploadPhoto(fileWithNullContentType);

        assertNotNull(result);
        assertEquals("photos/test.jpg", result);
        verify(mockS3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testDownloadPhoto_Success() throws IOException {
        String testKey = "photos/test-photo.jpg";
        byte[] testData = "test image data".getBytes();
        String contentType = "image/jpeg";

        InputStream mockInputStream = new ByteArrayInputStream(testData);
        GetObjectResponse mockResponse = GetObjectResponse.builder()
                .contentType(contentType)
                .build();

        software.amazon.awssdk.core.ResponseInputStream<GetObjectResponse> responseInputStream =
                new software.amazon.awssdk.core.ResponseInputStream<>(
                        mockResponse,
                        mockInputStream
                );

        when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(responseInputStream);

        ResponseEntity<byte[]> response = photoController.downloadPhoto(testKey);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(testData, response.getBody());

        HttpHeaders headers = response.getHeaders();
        assertEquals("inline", headers.getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(contentType, headers.getFirst(HttpHeaders.CONTENT_TYPE));

        verify(mockS3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void testDownloadPhoto_WithNullContentType() throws IOException {
        String testKey = "photos/test-photo.jpg";
        byte[] testData = "test image data".getBytes();

        InputStream mockInputStream = new ByteArrayInputStream(testData);
        GetObjectResponse mockResponse = GetObjectResponse.builder()
                .contentType(null)
                .build();

        software.amazon.awssdk.core.ResponseInputStream<GetObjectResponse> responseInputStream =
                new software.amazon.awssdk.core.ResponseInputStream<>(
                        mockResponse,
                        mockInputStream
                );

        when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(responseInputStream);

        ResponseEntity<byte[]> response = photoController.downloadPhoto(testKey);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(testData, response.getBody());

        HttpHeaders headers = response.getHeaders();
        assertEquals("inline", headers.getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertNull(headers.getFirst(HttpHeaders.CONTENT_TYPE));

        verify(mockS3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void testDownloadPhoto_EmptyKey() throws IOException {
        String emptyKey = "";
        byte[] testData = "test data".getBytes();

        InputStream mockInputStream = new ByteArrayInputStream(testData);
        GetObjectResponse mockResponse = GetObjectResponse.builder()
                .contentType("image/jpeg")
                .build();

        software.amazon.awssdk.core.ResponseInputStream<GetObjectResponse> responseInputStream =
                new software.amazon.awssdk.core.ResponseInputStream<>(
                        mockResponse,
                        mockInputStream
                );

        when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(responseInputStream);

        ResponseEntity<byte[]> response = photoController.downloadPhoto(emptyKey);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(testData, response.getBody());

        verify(mockS3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    void testDownloadPhoto_NullKey() throws IOException {
        String nullKey = null;
        byte[] testData = "test data".getBytes();

        InputStream mockInputStream = new ByteArrayInputStream(testData);
        GetObjectResponse mockResponse = GetObjectResponse.builder()
                .contentType("image/jpeg")
                .build();

        software.amazon.awssdk.core.ResponseInputStream<GetObjectResponse> responseInputStream =
                new software.amazon.awssdk.core.ResponseInputStream<>(
                        mockResponse,
                        mockInputStream
                );

        when(mockS3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(responseInputStream);

        ResponseEntity<byte[]> response = photoController.downloadPhoto(nullKey);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(testData, response.getBody());

        verify(mockS3Client).getObject(any(GetObjectRequest.class));
    }

}