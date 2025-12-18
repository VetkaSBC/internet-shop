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
import org.nicetu.spb.orderservice.model.entity.Cart;
import org.nicetu.spb.orderservice.model.entity.Order;
import org.nicetu.spb.orderservice.model.entity.OrderItem;
import org.nicetu.spb.orderservice.repository.CartRepository;
import org.nicetu.spb.orderservice.repository.OrderItemRepository;
import org.nicetu.spb.orderservice.repository.OrderRepository;
import org.nicetu.spb.orderservice.security.JwtProvider;
import org.nicetu.spb.orderservice.service.CallAPI;
import org.nicetu.spb.orderservice.service.EmailService;
import org.nicetu.spb.orderservice.service.InventoryService;
import org.nicetu.spb.orderservice.service.impl.OrderServiceImpl;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

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
    private OrderItem testOrderItem;
    private OrderItemDto testOrderItemDto;
    private Cart testCart;
    private ProductDto testProductDto;
    private List<Order> testOrders;
    private List<OrderItem> testOrderItems;

    @BeforeEach
    void setUp() {
        testCart = Cart.builder()
                .cartId(1)
                .userId(100L)
                .build();

        testOrder = Order.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test order")
                .orderFee(150.0)
                .status("NEW")
                .cartId(1)
                .build();

        testOrderItem = OrderItem.builder()
                .orderItemId(1)
                .productId(500L)
                .quantity(2)
                .price(75.0)
                .totalPrice(150.0)
                .orderId(1)
                .build();

        testOrderItemDto = OrderItemDto.builder()
                .orderItemId(1)
                .productId(500L)
                .quantity(2)
                .price(75.0)
                .totalPrice(150.0)
                .build();

        testProductDto = ProductDto.builder()
                .productId(500L)
                .productTitle("Test Product")
                .priceUnit(75.0)
                .quantity(100)
                .build();

        testOrderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test order")
                .orderFee(150.0)
                .status("NEW")
                .orderItemDtos(Set.of(testOrderItemDto))
                .build();

        testOrders = List.of(testOrder);
        testOrderItems = List.of(testOrderItem);
    }

    @Test
    void findAll_ShouldReturnPageOfOrders() {
        when(orderRepository.countAll()).thenReturn(Mono.just(1L));
        when(orderRepository.findAllWithPagination(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Flux.fromIterable(testOrders));
        when(orderItemRepository.findAllByOrderId(anyInt()))
                .thenReturn(Flux.fromIterable(testOrderItems));
        when(cartRepository.findById(anyInt()))
                .thenReturn(Mono.just(testCart));
        when(callAPI.receiverProductDto(anyLong()))
                .thenReturn(Mono.just(testProductDto));

        StepVerifier.create(orderService.findAll(0, 10, "orderId", "asc"))
                .expectNextMatches(pageResult -> {
                    assert pageResult.getTotalElements() == 1;
                    assert pageResult.getContent().size() == 1;
                    OrderDto orderDto = pageResult.getContent().get(0);
                    assert orderDto.getOrderId() == 1;
                    assert orderDto.getOrderFee() == 150.0;
                    return true;
                })
                .verifyComplete();

        verify(orderRepository).countAll();
        verify(orderRepository).findAllWithPagination("orderId", "ASC", 10, 0);
        verify(orderItemRepository).findAllByOrderId(1);
        verify(cartRepository).findById(1);
        verify(callAPI).receiverProductDto(500L);
    }

    @Test
    void findAll_ShouldReturnEmptyPage_WhenNoOrders() {
        when(orderRepository.countAll()).thenReturn(Mono.just(0L));

        StepVerifier.create(orderService.findAll(0, 10, "orderId", "asc"))
                .expectNextMatches(Page::isEmpty)
                .verifyComplete();

        verify(orderRepository).countAll();
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void findById_ShouldReturnOrder_WhenExists() {
        when(orderRepository.findById(1)).thenReturn(Mono.just(testOrder));
        when(orderItemRepository.findAllByOrderId(1))
                .thenReturn(Flux.fromIterable(testOrderItems));
        when(cartRepository.findById(1))
                .thenReturn(Mono.just(testCart));
        when(callAPI.receiverProductDto(500L))
                .thenReturn(Mono.just(testProductDto));

        StepVerifier.create(orderService.findById(1))
                .expectNextMatches(orderDto -> {
                    assert orderDto.getOrderId() == 1;
                    assert orderDto.getOrderFee() == 150.0;
                    assert orderDto.getOrderItemDtos() != null;
                    assert orderDto.getOrderItemDtos().size() == 1;
                    OrderItemDto item = orderDto.getOrderItemDtos().iterator().next();
                    assert item.getProductDto() != null;
                    assert item.getProductDto().getProductId() == 500L;
                    return true;
                })
                .verifyComplete();

        verify(orderRepository).findById(1);
        verify(orderItemRepository).findAllByOrderId(1);
        verify(cartRepository).findById(1);
        verify(callAPI).receiverProductDto(500L);
    }

    @Test
    void findById_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepository.findById(1)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.findById(1))
                .expectError(OrderNotFoundException.class)
                .verify();

        verify(orderRepository).findById(1);
        verifyNoInteractions(orderItemRepository, cartRepository, callAPI);
    }

    @Test
    void save_ShouldThrowException_WhenInventoryReservationFails() {
        OrderDto newOrderDto = OrderDto.builder()
                .orderId(null)
                .orderDate(LocalDateTime.now())
                .orderDesc("New order")
                .orderItemDtos(Set.of(testOrderItemDto))
                .build();

        doNothing().when(inventoryService).calculateOrderTotal(any(OrderDto.class));
        when(inventoryService.reserveProducts(any(OrderDto.class)))
                .thenReturn(Mono.just(false));

        StepVerifier.create(orderService.save(newOrderDto, "test-jwt-token"))
                .expectError(RuntimeException.class)
                .verify();

        verify(inventoryService).calculateOrderTotal(newOrderDto);
        verify(inventoryService).reserveProducts(newOrderDto);
        verifyNoInteractions(orderRepository, orderItemRepository, emailService);
    }

    @Test
    void save_ShouldThrowException_WhenOrderDtoIsNull() {
        StepVerifier.create(orderService.save(null, "token"))
                .expectError(IllegalArgumentException.class)
                .verify();

        verifyNoInteractions(inventoryService, orderRepository, emailService);
    }

    @Test
    void save_ShouldThrowException_WhenOrderItemsAreEmpty() {
        OrderDto invalidOrderDto = OrderDto.builder()
                .orderItemDtos(new HashSet<>())
                .build();

        StepVerifier.create(orderService.save(invalidOrderDto, "token"))
                .expectError(IllegalArgumentException.class)
                .verify();

        verifyNoInteractions(inventoryService, orderRepository, emailService);
    }

    @Test
    void save_ShouldThrowException_WhenOrderItemHasNoProductId() {
        OrderItemDto invalidItem = OrderItemDto.builder()
                .quantity(1)
                .price(10.0)
                .build();

        OrderDto invalidOrderDto = OrderDto.builder()
                .orderItemDtos(Set.of(invalidItem))
                .build();

        StepVerifier.create(orderService.save(invalidOrderDto, "token"))
                .expectError(IllegalArgumentException.class)
                .verify();

        verifyNoInteractions(inventoryService, orderRepository, emailService);
    }

    @Test
    void update_ShouldUpdateOrderSuccessfully() {
        OrderDto updatedOrderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Updated order")
                .orderFee(200.0)
                .status("UPDATED")
                .orderItemDtos(Set.of(testOrderItemDto))
                .build();

        OrderDto existingOrderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test order")
                .orderFee(150.0)
                .status("NEW")
                .orderItemDtos(Set.of(testOrderItemDto))
                .build();

        when(orderRepository.findById(1))
                .thenReturn(Mono.just(testOrder));

        when(orderItemRepository.findAllByOrderId(1))
                .thenReturn(Flux.fromIterable(testOrderItems));
        when(cartRepository.findById(1))
                .thenReturn(Mono.just(testCart));
        when(callAPI.receiverProductDto(500L))
                .thenReturn(Mono.just(testProductDto));

        when(inventoryService.releaseProducts(any(OrderDto.class)))
                .thenReturn(Mono.just(true));
        when(orderRepository.save(any(Order.class)))
                .thenReturn(Mono.just(testOrder));
        when(orderItemRepository.deleteAllByOrderId(1))
                .thenReturn(Mono.empty());
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenReturn(Mono.just(testOrderItem));
        when(inventoryService.reserveProducts(any(OrderDto.class)))
                .thenReturn(Mono.just(true));

        when(orderRepository.findById(1))
                .thenReturn(Mono.just(testOrder))
                .thenReturn(Mono.just(testOrder));

        StepVerifier.create(orderService.update(updatedOrderDto))
                .expectNextMatches(orderDto -> orderDto.getOrderId() == 1)
                .verifyComplete();

        verify(inventoryService).releaseProducts(any(OrderDto.class));
        verify(inventoryService).reserveProducts(any(OrderDto.class));
        verify(orderRepository, atLeast(2)).findById(1);
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).deleteAllByOrderId(1);
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    void update_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepository.findById(1)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.update(testOrderDto))
                .expectError(OrderNotFoundException.class)
                .verify();

        verify(orderRepository).findById(1);
        verifyNoMoreInteractions(orderRepository);
        verifyNoInteractions(inventoryService, orderItemRepository);
    }

    @Test
    void deleteById_ShouldDeleteOrderSuccessfully() {
        when(orderRepository.findById(1)).thenReturn(Mono.just(testOrder));

        when(orderItemRepository.findAllByOrderId(1))
                .thenReturn(Flux.fromIterable(testOrderItems));
        when(cartRepository.findById(1))
                .thenReturn(Mono.just(testCart));
        when(callAPI.receiverProductDto(500L))
                .thenReturn(Mono.just(testProductDto));

        when(inventoryService.releaseProducts(any(OrderDto.class)))
                .thenReturn(Mono.just(true));
        when(orderItemRepository.deleteAllByOrderId(1))
                .thenReturn(Mono.empty());
        when(orderRepository.deleteById(1))
                .thenReturn(Mono.empty());

        StepVerifier.create(orderService.deleteById(1))
                .verifyComplete();

        verify(inventoryService).releaseProducts(any(OrderDto.class));
        verify(orderItemRepository).deleteAllByOrderId(1);
        verify(orderRepository).deleteById(1);
    }

    @Test
    void deleteById_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepository.findById(1)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.deleteById(1))
                .expectError(OrderNotFoundException.class)
                .verify();

        verify(orderRepository).findById(1);
        verifyNoMoreInteractions(orderRepository);
        verifyNoInteractions(inventoryService, orderItemRepository);
    }

    @Test
    void existsByOrderId_ShouldReturnTrue_WhenOrderExists() {
        when(orderRepository.existsByOrderId(1)).thenReturn(Mono.just(true));

        StepVerifier.create(orderService.existsByOrderId(1))
                .expectNext(true)
                .verifyComplete();

        verify(orderRepository).existsByOrderId(1);
    }

    @Test
    void existsByOrderId_ShouldReturnFalse_WhenOrderNotExists() {
        when(orderRepository.existsByOrderId(1)).thenReturn(Mono.just(false));

        StepVerifier.create(orderService.existsByOrderId(1))
                .expectNext(false)
                .verifyComplete();

        verify(orderRepository).existsByOrderId(1);
    }
}