package org.nicetu.spb.productservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.productservice.model.dto.CategoryDto;
import org.nicetu.spb.productservice.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/paging")
    public Mono<ResponseEntity<Page<CategoryDto>>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("CategoryDto Page, controller; fetch categories with pagination: page={}, size={}", page, size);

        return categoryService.findAllCategory(page, size)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @GetMapping("/paging-and-sorting")
    public Mono<ResponseEntity<Page<CategoryDto>>> getAllCategoriesSorted(
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "categoryId") String sortBy) {

        log.info("CategoryDto Page, controller; fetch sorted categories: pageNo={}, pageSize={}, sortBy={}",
                pageNo, pageSize, sortBy);

        return categoryService.findAllCategory(pageNo, pageSize)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @GetMapping("/{categoryId}")
    public Mono<ResponseEntity<CategoryDto>> findById(
            @PathVariable("categoryId")
            @NotBlank(message = "Input must not be blank")
            @Valid final String categoryId) {
        log.info("CategoryDto, resource; fetch category by id: {}", categoryId);

        return categoryService.findById(Integer.parseInt(categoryId))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Mono<ResponseEntity<CategoryDto>> save(
            @RequestBody @NotNull(message = "Input must not be NULL")
            @Valid final CategoryDto categoryDto) {
        log.info("CategoryDto, resource; save category");

        return categoryService.save(categoryDto)
                .map(savedCategory -> ResponseEntity.status(201).body(savedCategory))
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @PutMapping
    public Mono<ResponseEntity<CategoryDto>> update(
            @RequestBody
            @NotNull(message = "Input must not be NULL")
            @Valid final CategoryDto categoryDto) {
        log.info("CategoryDto, resource; update category");

        return categoryService.update(categoryDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{categoryId}")
    public Mono<ResponseEntity<CategoryDto>> update(
            @PathVariable("categoryId")
            @NotBlank(message = "Input must not be blank")
            @Valid final String categoryId,
            @RequestBody @NotNull(message = "Input must not be NULL")
            @Valid final CategoryDto categoryDto) {
        log.info("CategoryDto, resource; update category with categoryId: {}", categoryId);

        return categoryService.update(Integer.parseInt(categoryId), categoryDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{categoryId}")
    public Mono<ResponseEntity<Void>> deleteById(
            @PathVariable("categoryId") final String categoryId) {
        log.info("Void, resource; delete category by id: {}", categoryId);

        return categoryService.deleteById(Integer.parseInt(categoryId))
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(e -> Mono.just(ResponseEntity.notFound().build()));
    }
}