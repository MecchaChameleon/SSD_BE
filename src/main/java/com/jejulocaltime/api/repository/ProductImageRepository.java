package com.jejulocaltime.api.repository;

import com.jejulocaltime.api.domain.ProductImage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

    int countByProductId(Long productId);
}
