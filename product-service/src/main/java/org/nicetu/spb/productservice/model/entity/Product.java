package org.nicetu.spb.productservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "products")
public class Product implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", unique = true, nullable = false)
    private Long productId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "product",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<ProductPhoto> productPhotos = new ArrayList<>();

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "quantity_status")
    private String quantityStatus;

    @Column(name = "price")
    private Long price;

    @Column(name = "discount")
    private Long discount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    public void setQuantityStatus(Integer quantity) {
        if (quantity == 0) {
            this.quantityStatus = QuantityStatus.NOT_IN_STOCK.toString();
        } else if (quantity > 0 && quantity <= 5) {
            this.quantityStatus = QuantityStatus.FEW.toString();
        } else {
            this.quantityStatus = QuantityStatus.IN_STOCK.toString();
        }
    }
}
