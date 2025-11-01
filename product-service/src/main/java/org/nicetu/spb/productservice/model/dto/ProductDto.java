package org.nicetu.spb.productservice.model.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

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

    private Long productId;
    private String title;
    private String description;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ProductPhotoDto> productPhotos;
    private Integer quantity;
    private String quantityStatus;
    private Double priceUnit;
    private Long discount;

    @JsonProperty("category")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private CategoryDto categoryDto;
}
