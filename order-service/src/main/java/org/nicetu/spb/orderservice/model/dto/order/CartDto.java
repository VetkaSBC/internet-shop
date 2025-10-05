package org.nicetu.spb.orderservice.model.dto.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.nicetu.spb.orderservice.model.dto.user.UserDto;


import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@Builder
public class CartDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer cartId;
    private Long userId;

    @JsonProperty("order")
    @JsonInclude(Include.NON_NULL)
    private Set<OrderDto> orderDtos;

    @JsonProperty("user")
    @JsonInclude(Include.NON_NULL)
    private UserDto userDto;

}