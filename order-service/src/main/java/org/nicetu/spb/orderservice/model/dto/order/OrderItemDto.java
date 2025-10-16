package org.nicetu.spb.orderservice.model.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nicetu.spb.orderservice.model.dto.product.ProductDto;

import java.io.Serial;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class OrderItemDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer orderItemId;
    private Long productId;
    private Integer quantity;
    private Double price;
    private Double totalPrice;

    @JsonProperty("product")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ProductDto productDto;

    @JsonProperty("order")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private OrderDto orderDto;
}