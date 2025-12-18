package org.nicetu.spb.productservice.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("categories")
public class Category {

    @Id
    @Column("category_id")
    private Integer categoryId;

    @Column("category_title")
    private String categoryTitle;

    @Column("parent_category_id")
    private Integer parentCategoryId;

    @Transient
    @Builder.Default
    private Set<Category> subCategories = new HashSet<>();

    @Transient
    @Builder.Default
    private Set<Product> products = new HashSet<>();

    public void addSubCategory(Category category) {
        this.subCategories.add(category);
        category.setParentCategoryId(this.categoryId);
    }
}