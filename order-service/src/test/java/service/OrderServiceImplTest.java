package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.orderservice.exception.wrapper.OrderNotFoundException;
import org.nicetu.spb.orderservice.model.dto.order.OrderDto;
import org.nicetu.spb.orderservice.model.dto.order.OrderItemDto;
import org.nicetu.spb.orderservice.model.dto.product.ProductDto;
import org.nicetu.spb.orderservice.model.entity.Order;
import org.nicetu.spb.orderservice.repository.OrderRepository;
import org.nicetu.spb.orderservice.security.JwtProvider;
import org.nicetu.spb.orderservice.service.CallAPI;
import org.nicetu.spb.orderservice.service.EmailService;
import org.nicetu.spb.orderservice.service.InventoryService;
import org.nicetu.spb.orderservice.service.impl.OrderServiceImpl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CallAPI callAPI;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderDto testOrderDto;
    private OrderItemDto testOrderItemDto;
    private ProductDto testProductDto;

    @BeforeEach
    void setUp() {
        testOrderItemDto = OrderItemDto.builder()
                .orderItemId(1)
                .productId(1L)
                .quantity(2)
                .price(100.0)
                .totalPrice(200.0)
                .build();

        testOrderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderFee(200.0)
                .status("NEW")
                .orderItemDtos(Set.of(testOrderItemDto))
                .build();

        testOrder = Order.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderFee(200.0)
                .status("NEW")
                .orderItems(Collections.emptySet())
                .build();

        testProductDto = ProductDto.builder()
                .productId(1L)
                .productTitle("Test Product")
                .quantity(10)
                .priceUnit(100.0)
                .build();
    }

    @Test
    void findAll_ShouldReturnListOfOrderDtos() {
        when(orderRepository.findAll()).thenReturn(List.of(testOrder));

        Mono<List<OrderDto>> result = orderService.findAll();

        StepVerifier.create(result)
                .expectNextMatches(orders -> {
                    assertEquals(1, orders.size());
                    return true;
                })
                .verifyComplete();

        verify(orderRepository).findAll();
        verify(callAPI, never()).receiverProductDto(anyLong());
    }

    @Test
    void findAll_WithPaging_ShouldReturnPageOfOrderDtos() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("orderId"));
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder));

        when(orderRepository.findAll(pageable)).thenReturn(orderPage);

        Mono<Page<OrderDto>> result = orderService.findAll(0, 10, "orderId", "asc");

        StepVerifier.create(result)
                .expectNextMatches(page -> {
                    assertEquals(1, page.getContent().size());
                    return true;
                })
                .verifyComplete();

        verify(orderRepository).findAll(pageable);
        verify(callAPI, never()).receiverProductDto(anyLong());
    }

    @Test
    void findById_WhenOrderExists_ShouldReturnOrderDto() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));

        Mono<OrderDto> result = orderService.findById(1);

        StepVerifier.create(result)
                .expectNextMatches(orderDto -> {
                    assertEquals(1, orderDto.getOrderId());
                    return true;
                })
                .verifyComplete();

        verify(orderRepository).findById(1);
        verify(callAPI, never()).receiverProductDto(anyLong());
    }

    @Test
    void findById_WhenOrderNotFound_ShouldThrowException() {
        when(orderRepository.findById(1)).thenReturn(Optional.empty());

        StepVerifier.create(orderService.findById(1))
                .expectError(OrderNotFoundException.class)
                .verify();

        verify(orderRepository).findById(1);
    }

    @Test
    void save_WithValidOrder_ShouldSaveAndReturnOrderDto() {
        when(inventoryService.reserveProducts(any(OrderDto.class))).thenReturn(Mono.just(true));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(jwtProvider.getEmailFromToken(anyString())).thenReturn("test@example.com");

        doReturn(Mono.empty()).when(emailService).sendOrderConfirmationEmail(any(OrderDto.class), anyString());

        Mono<OrderDto> result = orderService.save(testOrderDto, "test-token");

        StepVerifier.create(result)
                .expectNextMatches(orderDto -> orderDto != null)
                .verifyComplete();

        verify(inventoryService).reserveProducts(any(OrderDto.class));
        verify(orderRepository).save(any(Order.class));
        verify(emailService).sendOrderConfirmationEmail(any(OrderDto.class), anyString());
    }

    @Test
    void save_WithNullOrder_ShouldThrowException() {
        StepVerifier.create(orderService.save(null, "test-token"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void save_WithEmptyOrderItems_ShouldThrowException() {
        OrderDto emptyOrderDto = OrderDto.builder()
                .orderItemDtos(Collections.emptySet())
                .build();

        StepVerifier.create(orderService.save(emptyOrderDto, "test-token"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void update_ShouldUpdateAndReturnOrderDto() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));
        when(inventoryService.releaseProducts(any(OrderDto.class))).thenReturn(Mono.just(true));
        when(inventoryService.reserveProducts(any(OrderDto.class))).thenReturn(Mono.just(true));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Mono<OrderDto> result = orderService.update(testOrderDto);

        StepVerifier.create(result)
                .expectNextMatches(orderDto -> orderDto != null)
                .verifyComplete();

        verify(orderRepository).findById(1);
        verify(orderRepository).save(any(Order.class));
        verify(inventoryService).releaseProducts(any(OrderDto.class));
        verify(inventoryService).reserveProducts(any(OrderDto.class));
    }

    @Test
    void update_WithOrderId_ShouldUpdateAndReturnOrderDto() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));
        when(inventoryService.releaseProducts(any(OrderDto.class))).thenReturn(Mono.just(true));
        when(inventoryService.reserveProducts(any(OrderDto.class))).thenReturn(Mono.just(true));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Mono<OrderDto> result = orderService.update(1, testOrderDto);

        StepVerifier.create(result)
                .expectNextMatches(orderDto -> orderDto != null)
                .verifyComplete();

        verify(orderRepository).findById(1);
        verify(orderRepository).save(any(Order.class));
        verify(inventoryService, times(1)).releaseProducts(any(OrderDto.class));
        verify(inventoryService, times(1)).reserveProducts(any(OrderDto.class));
    }

    @Test
    void deleteById_ShouldDeleteOrder() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));
        when(inventoryService.releaseProducts(any(OrderDto.class))).thenReturn(Mono.just(true));

        doNothing().when(orderRepository).deleteById(1);

        Mono<Void> result = orderService.deleteById(1);

        StepVerifier.create(result)
                .verifyComplete();

        verify(orderRepository).findById(1);
        verify(inventoryService).releaseProducts(any(OrderDto.class));
        verify(orderRepository).deleteById(1);
    }

    @Test
    void existsByOrderId_WhenOrderExists_ShouldReturnTrue() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));

        Boolean result = orderService.existsByOrderId(1);

        assertTrue(result);
        verify(orderRepository).findById(1);
    }

    @Test
    void existsByOrderId_WhenOrderNotExists_ShouldReturnFalse() {
        when(orderRepository.findById(1)).thenReturn(Optional.empty());

        Boolean result = orderService.existsByOrderId(1);

        assertFalse(result);
        verify(orderRepository).findById(1);
    }

    @Test
    void save_WithInvalidOrderItem_ShouldThrowException() {
        OrderItemDto invalidOrderItem = OrderItemDto.builder()
                .productId(null)
                .quantity(2)
                .build();

        OrderDto invalidOrderDto = OrderDto.builder()
                .orderItemDtos(Set.of(invalidOrderItem))
                .build();

        StepVerifier.create(orderService.save(invalidOrderDto, "test-token"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void save_WhenProductReservationFails_ShouldThrowException() {
        when(inventoryService.reserveProducts(any(OrderDto.class)))
                .thenReturn(Mono.error(new RuntimeException("Product not found")));

        StepVerifier.create(orderService.save(testOrderDto, "test-token"))
                .expectError(RuntimeException.class)
                .verify();

        verify(inventoryService).reserveProducts(any(OrderDto.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void save_WhenEmailSendingFails_ShouldStillReturnOrder() {
        when(inventoryService.reserveProducts(any(OrderDto.class))).thenReturn(Mono.just(true));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(jwtProvider.getEmailFromToken(anyString())).thenReturn("test@example.com");

        doReturn(Mono.error(new RuntimeException("Email failed"))).when(emailService)
                .sendOrderConfirmationEmail(any(OrderDto.class), anyString());

        Mono<OrderDto> result = orderService.save(testOrderDto, "test-token");

        StepVerifier.create(result)
                .expectNextMatches(orderDto -> orderDto != null)
                .verifyComplete();

        verify(inventoryService).reserveProducts(any(OrderDto.class));
        verify(orderRepository).save(any(Order.class));
        verify(emailService).sendOrderConfirmationEmail(any(OrderDto.class), anyString());
    }
}