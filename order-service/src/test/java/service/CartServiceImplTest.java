package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.nicetu.spb.orderservice.exception.wrapper.CartNotFoundException;
import org.nicetu.spb.orderservice.model.dto.order.CartDto;
import org.nicetu.spb.orderservice.model.dto.user.UserDto;
import org.nicetu.spb.orderservice.model.entity.Cart;
import org.nicetu.spb.orderservice.repository.CartRepository;
import org.nicetu.spb.orderservice.repository.OrderRepository;
import org.nicetu.spb.orderservice.service.CallAPI;
import org.nicetu.spb.orderservice.service.impl.CartServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private CallAPI callAPI;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart testCart;
    private CartDto testCartDto;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testCart = Cart.builder()
                .cartId(1)
                .userId(1L)
                .orders(Collections.emptySet())
                .build();

        testCartDto = CartDto.builder()
                .cartId(1)
                .userId(1L)
                .orderDtos(Collections.emptySet())
                .userDto(UserDto.builder().id(1L).build())
                .build();

        testUserDto = UserDto.builder()
                .id(1L)
                .fullname("Test User")
                .email("test@example.com")
                .build();
    }

    @Test
    void findAll_ShouldReturnListOfCartDtos() {
        when(cartRepository.findAll()).thenReturn(List.of(testCart));
        when(callAPI.receiverUserDto(anyLong(), nullable(String.class)))
                .thenReturn(Mono.just(testUserDto));

        Mono<List<CartDto>> result = cartService.findAll();

        StepVerifier.create(result)
                .expectNextMatches(carts -> {
                    assertEquals(1, carts.size());
                    assertEquals(1, carts.get(0).getCartId());
                    return true;
                })
                .verifyComplete();

        verify(cartRepository).findAll();
        verify(callAPI).receiverUserDto(1L, null);
    }

    @Test
    void findAll_WithPaging_ShouldReturnPageOfCartDtos() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("cartId"));
        Page<Cart> cartPage = new PageImpl<>(List.of(testCart));

        when(cartRepository.findAll(pageable)).thenReturn(cartPage);
        when(callAPI.receiverUserDto(anyLong(), nullable(String.class)))
                .thenReturn(Mono.just(testUserDto));

        Mono<Page<CartDto>> result = cartService.findAll(0, 10, "cartId", "asc");

        StepVerifier.create(result)
                .expectNextMatches(page -> {
                    assertEquals(1, page.getContent().size());
                    assertEquals(1, page.getContent().get(0).getCartId());
                    return true;
                })
                .verifyComplete();

        verify(cartRepository).findAll(pageable);
        verify(callAPI).receiverUserDto(1L, null);
    }

    @Test
    void findById_WhenCartExists_ShouldReturnCartDto() {
        when(cartRepository.findById(1)).thenReturn(Optional.of(testCart));
        when(callAPI.receiverUserDto(anyLong(), nullable(String.class)))
                .thenReturn(Mono.just(testUserDto));

        Mono<CartDto> result = cartService.findById(1);

        StepVerifier.create(result)
                .expectNextMatches(cartDto -> {
                    assertEquals(1, cartDto.getCartId());
                    assertEquals(1L, cartDto.getUserId());
                    return true;
                })
                .verifyComplete();

        verify(cartRepository).findById(1);
        verify(callAPI).receiverUserDto(1L, null);
    }

    @Test
    void findById_WhenCartNotFound_ShouldThrowException() {
        when(cartRepository.findById(1)).thenReturn(Optional.empty());

        StepVerifier.create(cartService.findById(1))
                .expectError(CartNotFoundException.class)
                .verify();

        verify(cartRepository).findById(1);
        verify(callAPI, never()).receiverUserDto(anyLong(), anyString());
    }

    @Test
    void save_ShouldSaveAndReturnCartDto() {
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(modelMapper.map(any(CartDto.class), eq(Cart.class))).thenReturn(testCart);
        when(modelMapper.map(any(Cart.class), eq(CartDto.class))).thenReturn(testCartDto);

        Mono<CartDto> result = cartService.save(testCartDto);

        StepVerifier.create(result)
                .expectNext(testCartDto)
                .verifyComplete();

        verify(cartRepository).save(any(Cart.class));
        verify(callAPI, never()).receiverUserDto(anyLong(), anyString());
    }

    @Test
    void update_ShouldUpdateAndReturnCartDto() {
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        Mono<CartDto> result = cartService.update(testCartDto);

        StepVerifier.create(result)
                .expectNextMatches(cartDto -> cartDto != null)
                .verifyComplete();

        verify(cartRepository).save(any(Cart.class));
        verify(callAPI, never()).receiverUserDto(anyLong(), anyString());
    }

    @Test
    void update_WithCartId_ShouldUpdateAndReturnCartDto() {
        when(cartRepository.findById(1)).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        when(callAPI.receiverUserDto(anyLong(), nullable(String.class)))
                .thenReturn(Mono.just(testUserDto));

        Mono<CartDto> result = cartService.update(1, testCartDto);

        StepVerifier.create(result)
                .expectNextMatches(cartDto -> cartDto != null)
                .verifyComplete();

        verify(cartRepository).findById(1);
        verify(cartRepository).save(any(Cart.class));
        verify(callAPI).receiverUserDto(1L, null);
    }

    @Test
    void deleteById_ShouldDeleteCart() {
        when(cartRepository.findById(1)).thenReturn(Optional.of(testCart));
        doNothing().when(orderRepository).deleteAllByCart(testCart);
        doNothing().when(cartRepository).deleteById(1);

        Mono<Void> result = cartService.deleteById(1);

        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepository).findById(1);
        verify(orderRepository).deleteAllByCart(testCart);
        verify(cartRepository).deleteById(1);
        verify(callAPI, never()).receiverUserDto(anyLong(), anyString());
    }

    @Test
    void deleteById_WhenCartNotFound_ShouldComplete() {
        when(cartRepository.findById(1)).thenReturn(Optional.empty());

        Mono<Void> result = cartService.deleteById(1);

        StepVerifier.create(result)
                .verifyComplete();

        verify(cartRepository).findById(1);
        verify(orderRepository, never()).deleteAllByCart(any());
        verify(cartRepository, never()).deleteById(any());
        verify(callAPI, never()).receiverUserDto(anyLong(), anyString());
    }
}