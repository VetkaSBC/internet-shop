package org.nicetu.spb.productservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProductPhotoDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long photoId;
    @JsonProperty("product")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ProductDto productDto;
    private String photoLink;
}
