package org.nicetu.spb.orderservice.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.orderservice.model.dto.order.CartDto;
import org.nicetu.spb.orderservice.service.CartService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carts")
@Tag(name = "CartController", description = "Operations related to carts")
public class CartController {

    private final CartService cartService;

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('USER')")
    public Mono<ResponseEntity<Page<CartDto>>> findAll(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(defaultValue = "cartId") String sortBy,
                                                       @RequestParam(defaultValue = "asc") String sortOrder) {
        return cartService.findAll(page, size, sortBy, sortOrder)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }


    @GetMapping("/{cartId}")
    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
    public Mono<ResponseEntity<CartDto>> findById(@PathVariable("cartId") Integer cartId) {
        return cartService.findById(cartId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }



    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    public Mono<ResponseEntity<CartDto>> save(@RequestBody
                                              @NotNull(message = "Input must not be NULL!")
                                              @Valid final CartDto cartDto) {
        log.info("*** CartDto, resource; save cart *");
        return cartService.save(cartDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }


    @PutMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public Mono<ResponseEntity<CartDto>> update(@RequestBody
                                                @NotNull(message = "Input must not be NULL")
                                                @Valid final CartDto cartDto) {
        log.info("*** CartDto, resource; update cart *");
        return cartService.update(cartDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    @PutMapping("/{cartId}")
    @PreAuthorize("hasAuthority('USER')")
    public Mono<ResponseEntity<CartDto>> update(@PathVariable("cartId")
                                                @NotBlank(message = "Input must not be blank")
                                                @Valid final String cartId,
                                                @RequestBody
                                                @NotNull(message = "Input must not be NULL")
                                                @Valid final CartDto cartDto) {
        log.info("*** CartDto, resource; update cart with cartId *");
        return cartService.update(Integer.parseInt(cartId), cartDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{cartId}")
    @PreAuthorize("hasAuthority('USER')")
    public Mono<ResponseEntity<Boolean>> deleteById(@PathVariable("cartId") final String cartId) {
        log.info("*** Boolean, resource; delete cart by id *");
        return cartService.deleteById(Integer.parseInt(cartId))
                .thenReturn(ResponseEntity.ok(true))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body(false));
    }

}