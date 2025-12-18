package org.nicetu.spb.orderservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.orderservice.exception.wrapper.CartNotFoundException;
import org.nicetu.spb.orderservice.mapper.CartMappingHelper;
import org.nicetu.spb.orderservice.model.dto.order.CartDto;
import org.nicetu.spb.orderservice.model.entity.Cart;
import org.nicetu.spb.orderservice.repository.CartRepository;
import org.nicetu.spb.orderservice.repository.OrderRepository;
import org.nicetu.spb.orderservice.security.JwtTokenFilter;
import org.nicetu.spb.orderservice.service.CallAPI;
import org.nicetu.spb.orderservice.service.CartService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final CallAPI callAPI;

    @Override
    public Mono<Page<CartDto>> findAll(int page, int size, String sortBy, String sortOrder) {
        log.info("CartDto List, service; fetch all carts with paging and sorting");

        Sort.Direction direction = Sort.Direction.fromString(sortOrder);
        String sortDirection = direction.isAscending() ? "ASC" : "DESC";

        return cartRepository.count()
                .flatMap(total -> {
                    if (total == 0) {
                        return Mono.just(Page.<CartDto>empty());
                    }

                    return cartRepository.findAllWithPagination(sortBy, sortDirection, size, page * size)
                            .flatMapSequential(cart ->
                                    orderRepository.findAllByCartId(cart.getCartId())
                                            .collectList()
                                            .flatMap(orders ->
                                                    CartMappingHelper.mapToDto(cart, Flux.fromIterable(orders))
                                                            .flatMap(cartDto -> enrichCartWithUserInfo(cartDto))
                                            )
                            )
                            .collectList()
                            .map(content -> {
                                Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
                                return new PageImpl<>(content, pageable, total);
                            });
                })
                .doOnSuccess(pageResult ->
                        log.info("Found {} carts on page {}", pageResult.getContent().size(), page))
                .doOnError(error ->
                        log.error("Error fetching carts: {}", error.getMessage()));
    }

    @Override
    public Mono<CartDto> findById(Integer cartId) {
        log.info("CartDto, service; fetch cart by id");

        return cartRepository.findById(cartId)
                .switchIfEmpty(Mono.error(new CartNotFoundException(
                        String.format("Cart with id: %d not found", cartId))))
                .flatMap(cart ->
                        orderRepository.findAllByCartId(cartId)
                                .collectList()
                                .flatMap(orders ->
                                        CartMappingHelper.mapToDto(cart, Flux.fromIterable(orders))
                                                .flatMap(this::enrichCartWithUserInfo)
                                )
                );
    }

    private Mono<CartDto> enrichCartWithUserInfo(CartDto cartDto) {
        if (cartDto.getUserDto() == null || cartDto.getUserDto().getId() == null) {
            return Mono.just(cartDto);
        }

        return callAPI.receiverUserDto(cartDto.getUserDto().getId(),
                        JwtTokenFilter.getTokenFromRequest())
                .flatMap(userDto -> {
                    cartDto.setUserDto(userDto);
                    return Mono.just(cartDto);
                })
                .onErrorResume(throwable -> {
                    log.error("Error fetching user info: {}", throwable.getMessage());
                    return Mono.just(cartDto);
                });
    }

    @Override
    @Transactional
    public Mono<CartDto> save(final CartDto cartDto) {
        log.info("CartDto, service; save cart");

        Cart cart = CartMappingHelper.mapToEntity(cartDto);
        return cartRepository.save(cart)
                .flatMap(savedCart ->
                        CartMappingHelper.mapToDto(savedCart, Flux.empty())
                                .flatMap(this::enrichCartWithUserInfo)
                );
    }

    @Override
    @Transactional
    public Mono<CartDto> update(final CartDto cartDto) {
        log.info("CartDto, service; update cart");

        Cart cart = CartMappingHelper.mapToEntity(cartDto);
        return cartRepository.save(cart)
                .flatMap(savedCart ->
                        CartMappingHelper.mapToDto(savedCart, Flux.empty())
                                .flatMap(this::enrichCartWithUserInfo)
                );
    }

    @Override
    @Transactional
    public Mono<CartDto> update(final Integer cartId, final CartDto cartDto) {
        log.info("CartDto, service; update cart with cartId");

        return cartRepository.findById(cartId)
                .switchIfEmpty(Mono.error(new CartNotFoundException(
                        "Cart with id " + cartId + " not found")))
                .flatMap(existingCart -> {
                    existingCart.setUserId(cartDto.getUserId());
                    return cartRepository.save(existingCart);
                })
                .flatMap(updatedCart ->
                        CartMappingHelper.mapToDto(updatedCart, Flux.empty())
                                .flatMap(this::enrichCartWithUserInfo)
                );
    }

    @Override
    @Transactional
    public Mono<Void> deleteById(final Integer cartId) {
        log.info("Void, service; delete cart by id");

        return orderRepository.deleteAllByCartId(cartId)
                .then(cartRepository.deleteById(cartId))
                .then();
    }
}