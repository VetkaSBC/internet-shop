package controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nicetu.spb.orderservice.controller.OrderController;
import org.nicetu.spb.orderservice.model.dto.order.OrderDto;
import org.nicetu.spb.orderservice.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private OrderDto testOrderDto;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        testOrderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order")
                .orderFee(100.0)
                .status("NEW")
                .orderItemDtos(Collections.emptySet())
                .build();

        mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("Authorization", "Bearer test-token");
    }

    @Test
    void findAll_WithPaging_ShouldReturnPageOfOrderDtos() {
        
        Page<OrderDto> orderPage = new PageImpl<>(List.of(testOrderDto));
        when(orderService.findAll(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(orderPage));

        
        Mono<ResponseEntity<Page<OrderDto>>> result = orderController.findAll(0, 10, "orderId", "asc");

        
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(1, response.getBody().getContent().size());
                    return true;
                })
                .verifyComplete();

        verify(orderService).findAll(0, 10, "orderId", "asc");
    }

    @Test
    void findById_ShouldReturnOrderDto() {
        
        when(orderService.findById(1)).thenReturn(Mono.just(testOrderDto));

        
        ResponseEntity<Mono<OrderDto>> response = orderController.findById("1");

        
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        StepVerifier.create(response.getBody())
                .expectNext(testOrderDto)
                .verifyComplete();

        verify(orderService).findById(1);
    }

    @Test
    void save_ShouldSaveAndReturnOrderDto() {
        
        when(orderService.save(any(OrderDto.class), anyString())).thenReturn(Mono.just(testOrderDto));

        
        Mono<ResponseEntity<OrderDto>> result = orderController.save(testOrderDto, mockRequest);

        
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(testOrderDto, response.getBody());
                    return true;
                })
                .verifyComplete();

        verify(orderService).save(testOrderDto, "test-token");
    }

    @Test
    void save_WhenNoAuthorizationHeader_ShouldStillCallService() {
        
        mockRequest.removeHeader("Authorization");
        when(orderService.save(any(OrderDto.class), isNull())).thenReturn(Mono.just(testOrderDto));

        
        Mono<ResponseEntity<OrderDto>> result = orderController.save(testOrderDto, mockRequest);

        
        StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK)
                .verifyComplete();

        verify(orderService).save(testOrderDto, null);
    }

    @Test
    void update_ShouldUpdateAndReturnOrderDto() {
        
        when(orderService.update(any(OrderDto.class))).thenReturn(Mono.just(testOrderDto));

        
        Mono<ResponseEntity<OrderDto>> result = orderController.update(testOrderDto);

        
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(testOrderDto, response.getBody());
                    return true;
                })
                .verifyComplete();

        verify(orderService).update(testOrderDto);
    }

    @Test
    void update_WithOrderId_ShouldUpdateAndReturnOrderDto() {
        
        when(orderService.update(eq(1), any(OrderDto.class))).thenReturn(Mono.just(testOrderDto));

        
        Mono<ResponseEntity<OrderDto>> result = orderController.update("1", testOrderDto);

        
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(testOrderDto, response.getBody());
                    return true;
                })
                .verifyComplete();

        verify(orderService).update(1, testOrderDto);
    }

    @Test
    void deleteById_ShouldDeleteAndReturnTrue() {
        
        when(orderService.deleteById(1)).thenReturn(Mono.empty());

        
        Mono<ResponseEntity<Boolean>> result = orderController.deleteById("1");

        
        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertTrue(response.getBody());
                    return true;
                })
                .verifyComplete();

        verify(orderService).deleteById(1);
    }
}