package org.nicetu.spb.orderservice.model.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.nicetu.spb.orderservice.model.dto.order.OrderDto;

import java.io.Serial;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@Builder
public class ProductDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long productId;
    private String productTitle;
    private String imageUrl;
    private String sku;
    private Double priceUnit;
    private Integer quantity;

    @JsonProperty("category")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private CategoryDto categoryDto;

    @JsonProperty("order")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private OrderDto orderDto;

}
