package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.nicetu.spb.productservice.exception.wrapper.CategoryNotFoundException;
import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.nicetu.spb.productservice.model.entity.Category;
import org.nicetu.spb.productservice.repository.CategoryRepository;
import org.nicetu.spb.productservice.repository.CategoryRepositoryPagingAndSorting;
import org.nicetu.spb.productservice.service.impl.CategoryServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryRepositoryPagingAndSorting categoryRepositoryPagingAndSorting;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();

        categoryDto = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();
    }

    @Test
    void findAll_ShouldReturnFluxOfCategoryDtos() {
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(category));

        var result = categoryService.findAll();

        assertNotNull(result);
        verify(categoryRepository).findAll();
    }

    @Test
    void findAllCategory_ShouldReturnPageOfCategoryDtos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> categoryPage = new PageImpl<>(Arrays.asList(category));
        when(categoryRepository.findAll(pageable)).thenReturn(categoryPage);

        Page<CategoryDto> result = categoryService.findAllCategory(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(categoryRepository).findAll(pageable);
    }

    @Test
    void getAllCategories_ShouldReturnListOfCategoryDtos() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("categoryId"));
        Page<Category> categoryPage = new PageImpl<>(Arrays.asList(category));
        when(categoryRepositoryPagingAndSorting.findAllPagedAndSortedCategories(pageable))
                .thenReturn(categoryPage);
        when(modelMapper.map(category, CategoryDto.class)).thenReturn(categoryDto);

        List<CategoryDto> result = categoryService.getAllCategories(0, 10, "categoryId");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(categoryRepositoryPagingAndSorting).findAllPagedAndSortedCategories(pageable);
    }

    @Test
    void findById_WhenCategoryExists_ShouldReturnCategoryDto() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

        CategoryDto result = categoryService.findById(1);

        assertNotNull(result);
        verify(categoryRepository).findById(1);
    }

    @Test
    void findById_WhenCategoryNotExists_ShouldThrowException() {
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.findById(1));
        verify(categoryRepository).findById(1);
    }

    @Test
    void save_ShouldReturnMonoOfCategoryDto() {
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        var resultMono = categoryService.save(categoryDto);
        CategoryDto result = resultMono.block();

        assertNotNull(result);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void update_WhenCategoryExists_ShouldReturnUpdatedCategoryDto() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryDto result = categoryService.update(categoryDto);

        assertNotNull(result);
        verify(categoryRepository).findById(1);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void update_WhenCategoryNotExists_ShouldThrowException() {
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.update(categoryDto));
        verify(categoryRepository).findById(1);
    }

    @Test
    void updateWithId_WhenCategoryExists_ShouldReturnUpdatedCategoryDto() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryDto result = categoryService.update(1, categoryDto);

        assertNotNull(result);
        verify(categoryRepository).findById(1);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void deleteById_ShouldCallRepositoryDelete() {
        doNothing().when(categoryRepository).deleteById(1);

        categoryService.deleteById(1);

        verify(categoryRepository).deleteById(1);
    }
}