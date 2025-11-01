package org.nicetu.spb.productservice.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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

    @Column(name = "price_unit")
    private Double priceUnit;

    @Column(name = "discount")
    private Long discount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @PrePersist
    @PreUpdate
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