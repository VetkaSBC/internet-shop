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
@Table("orders")
public class Order {

    @Id
    @Column("order_id")
    private Integer orderId;

    @Column("order_date")
    private LocalDateTime orderDate;

    @Column("order_desc")
    private String orderDesc;

    @Column("order_fee")
    private Double orderFee;

    @Column("status")
    private String status;

    @Column("cart_id")
    private Integer cartId;

    @Transient
    @Builder.Default
    private Set<OrderItem> orderItems = new HashSet<>();

    @Transient
    private Cart cart;
}