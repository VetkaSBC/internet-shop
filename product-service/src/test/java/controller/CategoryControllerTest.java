package controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.productservice.controller.CategoryController;
import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.nicetu.spb.productservice.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        categoryDto = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();
    }

    @Test
    void findAll_ShouldReturnFluxOfCategories() {
        List<CategoryDto> categories = Arrays.asList(categoryDto);
        when(categoryService.findAll()).thenReturn(Flux.just(categories));

        ResponseEntity<Flux<List<CategoryDto>>> response = categoryController.findAll();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(categoryService).findAll();
    }

    @Test
    void getAllCategoriesWithPaging_ShouldReturnPageOfCategories() {
        Page<CategoryDto> categoryPage = new PageImpl<>(Arrays.asList(categoryDto));
        when(categoryService.findAllCategory(0, 10)).thenReturn(categoryPage);

        ResponseEntity<Page<CategoryDto>> response = categoryController.getAllCategories(0, 10);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(categoryService).findAllCategory(0, 10);
    }

    @Test
    void getAllCategoriesWithPagingAndSorting_ShouldReturnListOfCategories() {
        List<CategoryDto> categories = Arrays.asList(categoryDto);
        when(categoryService.getAllCategories(0, 10, "categoryId")).thenReturn(categories);

        ResponseEntity<List<CategoryDto>> response = categoryController.getAllEmployees(0, 10, "categoryId");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(categoryService).getAllCategories(0, 10, "categoryId");
    }

    @Test
    void findById_ShouldReturnCategory() {
        when(categoryService.findById(1)).thenReturn(categoryDto);

        ResponseEntity<CategoryDto> response = categoryController.findById("1");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(categoryService).findById(1);
    }

    @Test
    void save_ShouldReturnSavedCategory() {
        when(categoryService.save(any(CategoryDto.class))).thenReturn(Mono.just(categoryDto));

        ResponseEntity<Mono<CategoryDto>> response = categoryController.save(categoryDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(categoryService).save(any(CategoryDto.class));
    }

    @Test
    void update_ShouldReturnUpdatedCategory() {
        when(categoryService.update(any(CategoryDto.class))).thenReturn(categoryDto);

        ResponseEntity<CategoryDto> response = categoryController.update(categoryDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(categoryService).update(any(CategoryDto.class));
    }

    @Test
    void updateWithId_ShouldReturnUpdatedCategory() {
        when(categoryService.update(anyInt(), any(CategoryDto.class))).thenReturn(categoryDto);

        ResponseEntity<CategoryDto> response = categoryController.update("1", categoryDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(categoryService).update(1, categoryDto);
    }

    @Test
    void deleteById_ShouldReturnTrue() {
        doNothing().when(categoryService).deleteById(1);

        ResponseEntity<Boolean> response = categoryController.deleteById("1");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody());
        verify(categoryService).deleteById(1);
    }
}