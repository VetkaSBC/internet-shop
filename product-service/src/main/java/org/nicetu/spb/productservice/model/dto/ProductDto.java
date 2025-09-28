package org.nicetu.spb.productservice.model.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@Builder
public class ProductDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Product ID cannot be null")
    private Long productId;
    private String title;
    private String description;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ProductPhotoDto> routePhotos;
    private Integer quantity;
    private String quantityStatus;
    private Long price;
    private Long discount;

    @JsonProperty("category")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private CategoryDto categoryDto;
}
