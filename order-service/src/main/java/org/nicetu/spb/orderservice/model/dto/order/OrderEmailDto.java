package org.nicetu.spb.orderservice.model.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEmailDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String customerName;
    private String customerEmail;
    private Integer orderId;
    private LocalDateTime orderDate;
    private String orderDesc;
    private Double orderFee;
    private Set<OrderItemDto> orderItems;
}
