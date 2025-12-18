package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.productservice.exception.wrapper.CategoryNotFoundException;
import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.nicetu.spb.productservice.model.entity.Category;
import org.nicetu.spb.productservice.repository.CategoryRepository;
import org.nicetu.spb.productservice.repository.ProductCategoryRepository;
import org.nicetu.spb.productservice.service.impl.CategoryServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private CategoryDto testCategoryDto;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategoryDto = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();

        testCategory = Category.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();
    }

    @Test
    void findById_shouldReturnCategoryWhenExists() {
        when(categoryRepository.findById(1)).thenReturn(Mono.just(testCategory));
        when(categoryRepository.findByParentCategoryId(anyInt())).thenReturn(Flux.empty());
        when(productCategoryRepository.findProductIdsByCategoryId(anyInt())).thenReturn(Flux.empty());

        Mono<CategoryDto> result = categoryService.findById(1);

        StepVerifier.create(result)
                .assertNext(category -> {
                    assertThat(category.getCategoryId()).isEqualTo(1);
                    assertThat(category.getCategoryTitle()).isEqualTo("Electronics");
                })
                .verifyComplete();

        verify(categoryRepository).findById(1);
    }

    @Test
    void findById_shouldThrowExceptionWhenNotFound() {
        when(categoryRepository.findById(999)).thenReturn(Mono.empty());

        Mono<CategoryDto> result = categoryService.findById(999);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof CategoryNotFoundException &&
                                throwable.getMessage().contains("Category not found"))
                .verify();

        verify(categoryRepository).findById(999);
    }

    @Test
    void findAllCategory_shouldReturnPagedCategories() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Category> categories = List.of(testCategory);
        Page<Category> categoryPage = new PageImpl<>(categories, pageable, 1);

        when(categoryRepository.findAllBy(any(Pageable.class))).thenReturn(Flux.fromIterable(categories));
        when(categoryRepository.count()).thenReturn(Mono.just(1L));
        when(categoryRepository.findByParentCategoryId(anyInt())).thenReturn(Flux.empty());
        when(productCategoryRepository.findProductIdsByCategoryId(anyInt())).thenReturn(Flux.empty());

        Mono<Page<CategoryDto>> result = categoryService.findAllCategory(0, 10);

        StepVerifier.create(result)
                .assertNext(page -> {
                    assertThat(page.getTotalElements()).isEqualTo(1);
                    assertThat(page.getContent()).hasSize(1);
                    assertThat(page.getContent().get(0).getCategoryTitle()).isEqualTo("Electronics");
                })
                .verifyComplete();

        verify(categoryRepository).findAllBy(any(Pageable.class));
        verify(categoryRepository).count();
    }

    @Test
    void save_shouldSaveCategorySuccessfully() {
        when(categoryRepository.save(any(Category.class))).thenReturn(Mono.just(testCategory));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });
        when(categoryRepository.findByParentCategoryId(anyInt())).thenReturn(Flux.empty());
        when(productCategoryRepository.findProductIdsByCategoryId(anyInt())).thenReturn(Flux.empty());

        Mono<CategoryDto> result = categoryService.save(testCategoryDto);

        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertThat(saved.getCategoryId()).isEqualTo(1);
                    assertThat(saved.getCategoryTitle()).isEqualTo("Electronics");
                })
                .verifyComplete();

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void update_shouldUpdateCategorySuccessfully() {
        CategoryDto updateDto = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Updated Electronics")
                .build();

        Category updatedCategory = Category.builder()
                .categoryId(1)
                .categoryTitle("Updated Electronics")
                .build();

        when(categoryRepository.findById(1)).thenReturn(Mono.just(testCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(Mono.just(updatedCategory));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });
        when(categoryRepository.findByParentCategoryId(anyInt())).thenReturn(Flux.empty());
        when(productCategoryRepository.findProductIdsByCategoryId(anyInt())).thenReturn(Flux.empty());

        Mono<CategoryDto> result = categoryService.update(updateDto);

        StepVerifier.create(result)
                .assertNext(updated -> {
                    assertThat(updated.getCategoryTitle()).isEqualTo("Updated Electronics");
                })
                .verifyComplete();

        verify(categoryRepository).findById(1);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void deleteById_shouldDeleteCategorySuccessfully() {
        when(categoryRepository.findById(1)).thenReturn(Mono.just(testCategory));
        when(categoryRepository.hasSubcategories(1)).thenReturn(Mono.just(false));
        when(productCategoryRepository.deleteByCategoryId(1)).thenReturn(Mono.just(1));
        when(categoryRepository.delete(any(Category.class))).thenReturn(Mono.empty());
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono;
        });

        Mono<Void> result = categoryService.deleteById(1);

        StepVerifier.create(result)
                .verifyComplete();

        verify(categoryRepository).findById(1);
        verify(categoryRepository).hasSubcategories(1);
        verify(productCategoryRepository).deleteByCategoryId(1);
        verify(categoryRepository).delete(any(Category.class));
    }
}