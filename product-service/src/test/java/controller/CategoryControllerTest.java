package controller;

import org.junit.jupiter.api.Test;
import org.nicetu.spb.productservice.controller.CategoryController;
import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.nicetu.spb.productservice.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest
@ContextConfiguration(classes = CategoryControllerTest.TestConfig.class)
class CategoryControllerTest {

    @Configuration
    static class TestConfig {
        @Bean
        public CategoryController categoryController(CategoryService categoryService) {
            return new CategoryController(categoryService);
        }

        @Bean
        public CategoryService categoryService() {
            return mock(CategoryService.class);
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CategoryService categoryService;

    @Test
    void getCategoryById_shouldReturnCategory() {
        CategoryDto categoryDto = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();

        when(categoryService.findById(1)).thenReturn(Mono.just(categoryDto));

        webTestClient.get()
                .uri("/api/categories/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.categoryId").isEqualTo(1)
                .jsonPath("$.categoryTitle").isEqualTo("Electronics");
    }

    @Test
    void getCategoryById_shouldReturnNotFound() {
        when(categoryService.findById(999)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/categories/{id}", 999)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getAllCategories_shouldReturnCategories() {
        CategoryDto categoryDto = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();

        Page<CategoryDto> page = new PageImpl<>(List.of(categoryDto), PageRequest.of(0, 10), 1);
        when(categoryService.findAllCategory(anyInt(), anyInt())).thenReturn(Mono.just(page));

        webTestClient.get()
                .uri("/api/categories/paging?page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].categoryId").isEqualTo(1)
                .jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void getAllCategoriesSorted_shouldReturnSortedCategories() {
        CategoryDto categoryDto = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();

        Page<CategoryDto> page = new PageImpl<>(List.of(categoryDto), PageRequest.of(0, 10), 1);
        when(categoryService.findAllCategory(anyInt(), anyInt())).thenReturn(Mono.just(page));

        webTestClient.get()
                .uri("/api/categories/paging-and-sorting?pageNo=0&pageSize=10&sortBy=categoryId")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].categoryId").isEqualTo(1);
    }

    @Test
    void createCategory_shouldReturnCreated() {
        CategoryDto request = CategoryDto.builder()
                .categoryTitle("Electronics")
                .build();

        CategoryDto response = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();

        when(categoryService.save(any(CategoryDto.class))).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.categoryId").isEqualTo(1)
                .jsonPath("$.categoryTitle").isEqualTo("Electronics");
    }

    @Test
    void updateCategory_shouldReturnUpdatedCategory() {
        CategoryDto request = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Updated Electronics")
                .build();

        when(categoryService.update(any(CategoryDto.class))).thenReturn(Mono.just(request));

        webTestClient.put()
                .uri("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.categoryId").isEqualTo(1)
                .jsonPath("$.categoryTitle").isEqualTo("Updated Electronics");
    }

    @Test
    void updateCategoryById_shouldReturnUpdatedCategory() {
        CategoryDto request = CategoryDto.builder()
                .categoryTitle("Updated Electronics")
                .build();

        CategoryDto response = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Updated Electronics")
                .build();

        when(categoryService.update(1, request)).thenReturn(Mono.just(response));

        webTestClient.put()
                .uri("/api/categories/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.categoryId").isEqualTo(1)
                .jsonPath("$.categoryTitle").isEqualTo("Updated Electronics");
    }

    @Test
    void deleteCategory_shouldReturnNoContent() {
        when(categoryService.deleteById(1)).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/categories/{id}", 1)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void deleteCategory_shouldReturnNotFound() {
        when(categoryService.deleteById(999)).thenReturn(Mono.error(new RuntimeException("Not found")));

        webTestClient.delete()
                .uri("/api/categories/{id}", 999)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getAllCategories_withoutPaging_shouldReturnBadRequest() {
        webTestClient.get()
                .uri("/api/categories")
                .exchange()
                .expectStatus().is4xxClientError();
    }


    @Test
    void updateCategory_withInvalidId_shouldReturnNotFound() {
        CategoryDto request = CategoryDto.builder()
                .categoryId(999)
                .categoryTitle("Not Found")
                .build();

        when(categoryService.update(any(CategoryDto.class))).thenReturn(Mono.empty());

        webTestClient.put()
                .uri("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }


}