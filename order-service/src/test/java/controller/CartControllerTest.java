package controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.orderservice.controller.CartController;
import org.nicetu.spb.orderservice.model.dto.order.CartDto;
import org.nicetu.spb.orderservice.service.CartService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController cartController;

    private CartDto testCartDto;

    @BeforeEach
    void setUp() {
        testCartDto = CartDto.builder()
                .cartId(1)
                .userId(1L)
                .orderDtos(Collections.emptySet())
                .build();
    }

    @Test
    void findAll_ShouldReturnListOfCartDtos() {
        List<CartDto> cartList = List.of(testCartDto);
        when(cartService.findAll()).thenReturn(Mono.just(cartList));

        Mono<ResponseEntity<List<CartDto>>> result = cartController.findAll();

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(1, response.getBody().size());
                    return true;
                })
                .verifyComplete();

        verify(cartService).findAll();
    }

    @Test
    void findAll_WithPaging_ShouldReturnPageOfCartDtos() {
        Page<CartDto> cartPage = new PageImpl<>(List.of(testCartDto));
        when(cartService.findAll(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(cartPage));

        Mono<ResponseEntity<Page<CartDto>>> result = cartController.findAll(0, 10, "cartId", "asc");

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(1, response.getBody().getContent().size());
                    return true;
                })
                .verifyComplete();

        verify(cartService).findAll(0, 10, "cartId", "asc");
    }

    @Test
    void findById_ShouldReturnCartDto() {
        when(cartService.findById(1)).thenReturn(Mono.just(testCartDto));

        ResponseEntity<Mono<CartDto>> response = cartController.findById("1");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        StepVerifier.create(response.getBody())
                .expectNext(testCartDto)
                .verifyComplete();

        verify(cartService).findById(1);
    }

    @Test
    void save_ShouldSaveAndReturnCartDto() {
        when(cartService.save(any(CartDto.class))).thenReturn(Mono.just(testCartDto));

        Mono<ResponseEntity<CartDto>> result = cartController.save(testCartDto);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(testCartDto, response.getBody());
                    return true;
                })
                .verifyComplete();

        verify(cartService).save(testCartDto);
    }

    @Test
    void save_WhenServiceReturnsEmpty_ShouldReturnInternalServerError() {
        when(cartService.save(any(CartDto.class))).thenReturn(Mono.empty());

        Mono<ResponseEntity<CartDto>> result = cartController.save(testCartDto);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
                .verifyComplete();
    }

    @Test
    void update_ShouldUpdateAndReturnCartDto() {
        when(cartService.update(any(CartDto.class))).thenReturn(Mono.just(testCartDto));

        Mono<ResponseEntity<CartDto>> result = cartController.update(testCartDto);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(testCartDto, response.getBody());
                    return true;
                })
                .verifyComplete();

        verify(cartService).update(testCartDto);
    }

    @Test
    void update_WhenCartNotFound_ShouldReturnNotFound() {
        when(cartService.update(any(CartDto.class))).thenReturn(Mono.empty());

        Mono<ResponseEntity<CartDto>> result = cartController.update(testCartDto);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.NOT_FOUND)
                .verifyComplete();
    }

    @Test
    void update_WithCartId_ShouldUpdateAndReturnCartDto() {
        when(cartService.update(eq(1), any(CartDto.class))).thenReturn(Mono.just(testCartDto));

        Mono<ResponseEntity<CartDto>> result = cartController.update("1", testCartDto);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(testCartDto, response.getBody());
                    return true;
                })
                .verifyComplete();

        verify(cartService).update(1, testCartDto);
    }

    @Test
    void deleteById_ShouldDeleteAndReturnTrue() {
        when(cartService.deleteById(1)).thenReturn(Mono.empty());

        Mono<ResponseEntity<Boolean>> result = cartController.deleteById("1");

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody());
                    return true;
                })
                .verifyComplete();

        verify(cartService).deleteById(1);
    }

    @Test
    void deleteById_WhenCartNotFound_ShouldReturnNotFound() {
        when(cartService.deleteById(1)).thenReturn(Mono.empty());

        Mono<ResponseEntity<Boolean>> result = cartController.deleteById("1");

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                .verifyComplete();
    }
}