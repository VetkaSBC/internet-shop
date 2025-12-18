package service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.orderservice.exception.wrapper.CartNotFoundException;
import org.nicetu.spb.orderservice.model.dto.order.CartDto;
import org.nicetu.spb.orderservice.model.dto.user.UserDto;
import org.nicetu.spb.orderservice.model.entity.Cart;
import org.nicetu.spb.orderservice.model.entity.Order;
import org.nicetu.spb.orderservice.repository.CartRepository;
import org.nicetu.spb.orderservice.repository.OrderRepository;
import org.nicetu.spb.orderservice.security.JwtTokenFilter;
import org.nicetu.spb.orderservice.service.CallAPI;
import org.nicetu.spb.orderservice.service.impl.CartServiceImpl;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;


import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CallAPI callAPI;

    @InjectMocks
    private CartServiceImpl cartService;

    private Cart testCart;
    private CartDto testCartDto;
    private UserDto testUserDto;
    private List<Order> testOrders;

    private MockedStatic<JwtTokenFilter> jwtTokenFilterMockedStatic;

    @BeforeEach
    void setUp() {
        testCart = Cart.builder()
                .cartId(1)
                .userId(100L)
                .build();

        testUserDto = UserDto.builder()
                .id(100L)
                .fullname("John Doe")
                .email("john@example.com")
                .build();

        testCartDto = CartDto.builder()
                .cartId(1)
                .userId(100L)
                .userDto(testUserDto)
                .orderDtos(new HashSet<>())
                .build();

        testOrders = List.of(
                Order.builder()
                        .orderId(1)
                        .orderDate(LocalDateTime.now())
                        .orderDesc("Test order 1")
                        .orderFee(100.0)
                        .status("NEW")
                        .cartId(1)
                        .build()
        );

        jwtTokenFilterMockedStatic = Mockito.mockStatic(JwtTokenFilter.class);
        jwtTokenFilterMockedStatic.when(JwtTokenFilter::getTokenFromRequest)
                .thenReturn("test-token");
    }

    @AfterEach
    void tearDown() {
        if (jwtTokenFilterMockedStatic != null) {
            jwtTokenFilterMockedStatic.close();
        }
    }

    @Test
    void findAll_ShouldReturnPageOfCarts() {
        int page = 0;
        int size = 10;
        String sortBy = "cartId";
        String sortOrder = "asc";

        when(cartRepository.count()).thenReturn(Mono.just(1L));
        when(cartRepository.findAllWithPagination(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Flux.just(testCart));
        when(orderRepository.findAllByCartId(anyInt()))
                .thenReturn(Flux.fromIterable(testOrders));
        when(callAPI.receiverUserDto(anyLong(), anyString()))
                .thenReturn(Mono.just(testUserDto));

        StepVerifier.create(cartService.findAll(page, size, sortBy, sortOrder))
                .expectNextMatches(pageResult -> pageResult.getContent().size() == 1)
                .verifyComplete();

        verify(cartRepository).count();
        verify(cartRepository).findAllWithPagination(sortBy, "ASC", size, 0);
        verify(orderRepository).findAllByCartId(1);
        verify(callAPI).receiverUserDto(100L, "test-token");
        jwtTokenFilterMockedStatic.verify(JwtTokenFilter::getTokenFromRequest, atLeastOnce());
    }

    @Test
    void findAll_ShouldReturnEmptyPage_WhenNoCarts() {
        when(cartRepository.count()).thenReturn(Mono.just(0L));

        StepVerifier.create(cartService.findAll(0, 10, "cartId", "asc"))
                .expectNextMatches(Page::isEmpty)
                .verifyComplete();

        verify(cartRepository).count();
        verifyNoInteractions(orderRepository, callAPI);
    }

    @Test
    void findById_ShouldReturnCart_WhenExists() {
        when(cartRepository.findById(1)).thenReturn(Mono.just(testCart));
        when(orderRepository.findAllByCartId(1)).thenReturn(Flux.fromIterable(testOrders));
        when(callAPI.receiverUserDto(100L, "test-token")).thenReturn(Mono.just(testUserDto));

        StepVerifier.create(cartService.findById(1))
                .expectNextMatches(cartDto -> cartDto.getCartId() == 1)
                .verifyComplete();

        verify(cartRepository).findById(1);
        verify(orderRepository).findAllByCartId(1);
        verify(callAPI).receiverUserDto(100L, "test-token");
    }

    @Test
    void findById_ShouldThrowException_WhenCartNotFound() {
        when(cartRepository.findById(1)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.findById(1))
                .expectError(CartNotFoundException.class)
                .verify();

        verify(cartRepository).findById(1);
        verifyNoInteractions(orderRepository, callAPI);
    }

    @Test
    void findById_ShouldReturnCartWithoutUserInfo_WhenUserServiceFails() {
        when(cartRepository.findById(1)).thenReturn(Mono.just(testCart));
        when(orderRepository.findAllByCartId(1)).thenReturn(Flux.fromIterable(testOrders));
        when(callAPI.receiverUserDto(100L, "test-token"))
                .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        StepVerifier.create(cartService.findById(1))
                .expectNextMatches(cartDto -> cartDto.getCartId() == 1)
                .verifyComplete();

        verify(callAPI).receiverUserDto(100L, "test-token");
    }

    @Test
    void save_ShouldSaveAndReturnCart() {
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(testCart));
        when(callAPI.receiverUserDto(100L, "test-token")).thenReturn(Mono.just(testUserDto));

        StepVerifier.create(cartService.save(testCartDto))
                .expectNextMatches(cartDto -> cartDto.getCartId() == 1)
                .verifyComplete();

        verify(cartRepository).save(any(Cart.class));
        verify(callAPI).receiverUserDto(100L, "test-token");
    }

    @Test
    void update_ShouldUpdateAndReturnCart() {
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(testCart));
        when(callAPI.receiverUserDto(100L, "test-token")).thenReturn(Mono.just(testUserDto));

        StepVerifier.create(cartService.update(testCartDto))
                .expectNextMatches(cartDto -> cartDto.getCartId() == 1)
                .verifyComplete();

        verify(cartRepository).save(any(Cart.class));
        verify(callAPI).receiverUserDto(100L, "test-token");
    }

    @Test
    void updateWithId_ShouldUpdateExistingCart() {
        Cart updatedCart = Cart.builder()
                .cartId(1)
                .userId(200L)
                .build();

        UserDto updatedUserDto = UserDto.builder()
                .id(200L)
                .fullname("Jane Doe")
                .email("jane@example.com")
                .build();

        when(cartRepository.findById(1)).thenReturn(Mono.just(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(updatedCart));
        when(callAPI.receiverUserDto(200L, "test-token")).thenReturn(Mono.just(updatedUserDto));

        CartDto updateDto = CartDto.builder()
                .cartId(1)
                .userId(200L)
                .build();

        StepVerifier.create(cartService.update(1, updateDto))
                .expectNextMatches(cartDto -> cartDto.getCartId() == 1)
                .verifyComplete();

        verify(cartRepository).findById(1);
        verify(cartRepository).save(any(Cart.class));
        verify(callAPI).receiverUserDto(200L, "test-token");
    }

    @Test
    void updateWithId_ShouldThrowException_WhenCartNotFound() {
        when(cartRepository.findById(1)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.update(1, testCartDto))
                .expectError(CartNotFoundException.class)
                .verify();

        verify(cartRepository).findById(1);
        verifyNoMoreInteractions(cartRepository);
        verifyNoInteractions(callAPI);
    }

    @Test
    void deleteById_ShouldDeleteCartAndOrders() {
        when(orderRepository.deleteAllByCartId(1)).thenReturn(Mono.empty());
        when(cartRepository.deleteById(1)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.deleteById(1))
                .verifyComplete();

        verify(orderRepository).deleteAllByCartId(1);
        verify(cartRepository).deleteById(1);
    }

    @Test
    void deleteById_ShouldComplete_WhenCartDeletionFails() {
        when(orderRepository.deleteAllByCartId(1)).thenReturn(Mono.empty());
        when(cartRepository.deleteById(1))
                .thenReturn(Mono.error(new RuntimeException("Cart deletion failed")));

        StepVerifier.create(cartService.deleteById(1))
                .expectError(RuntimeException.class)
                .verify();

        verify(orderRepository).deleteAllByCartId(1);
        verify(cartRepository).deleteById(1);
    }
}