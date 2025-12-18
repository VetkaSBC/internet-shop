package org.nicetu.spb.productservice.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("products")
public class Product {

    @Id
    @Column("product_id")
    private Long productId;

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("quantity")
    private Integer quantity;

    @Column("quantity_status")
    private String quantityStatus;

    @Column("price_unit")
    private Double priceUnit;

    @Column("discount")
    private Long discount;

    @Version
    @Column("version")
    private Integer version;

    @Transient
    @Builder.Default
    private List<ProductPhoto> productPhotos = new ArrayList<>();

    @Transient
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    @Transient
    @Builder.Default
    private Set<Long> categoryIds = new HashSet<>();

    public void calculateQuantityStatus() {
        if (this.quantity == null) {
            this.quantityStatus = QuantityStatus.NOT_IN_STOCK.toString();
            return;
        }

        if (this.quantity == 0) {
            this.quantityStatus = QuantityStatus.NOT_IN_STOCK.toString();
        } else if (this.quantity > 0 && this.quantity <= 5) {
            this.quantityStatus = QuantityStatus.FEW.toString();
        } else {
            this.quantityStatus = QuantityStatus.IN_STOCK.toString();
        }
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.calculateQuantityStatus();
    }
}