package org.nicetu.spb.productservice.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("product_photo")
public class ProductPhoto {

    @Id
    @Column("photo_id")
    private Long photoId;

    @Column("product_id")
    private Long productId;

    @Column("photo_link")
    private String photoLink;

    @Column("original_file_name")
    private String originalFileName;

    @Column("file_size")
    private Long fileSize;

    @Column("content_type")
    private String contentType;

    @Column("uploaded_at")
    private LocalDateTime uploadedAt;

    @Transient
    private Product product;
}