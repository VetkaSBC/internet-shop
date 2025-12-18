package org.nicetu.spb.orderservice.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("order_items")
public class OrderItem {

    @Id
    @Column("order_item_id")
    private Integer orderItemId;

    @Column("product_id")
    private Long productId;

    @Column("quantity")
    private Integer quantity;

    @Column("price")
    private Double price;

    @Column("total_price")
    private Double totalPrice;

    @Column("order_id")
    private Integer orderId;
}