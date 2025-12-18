package org.nicetu.spb.orderservice.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("carts")
public class Cart {

    @Id
    @Column("cart_id")
    private Integer cartId;

    @Column("user_id")
    private Long userId;

    @Transient
    @Builder.Default
    private Set<Order> orders = new HashSet<>();
}