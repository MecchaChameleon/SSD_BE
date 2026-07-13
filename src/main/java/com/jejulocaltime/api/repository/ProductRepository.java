package com.jejulocaltime.api.repository;

import com.jejulocaltime.api.domain.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findBySellerProfileId(Long sellerProfileId, Pageable pageable);

    Page<Product> findBySellerProfileIdAndStatus(Long sellerProfileId, Product.Status status, Pageable pageable);

    Optional<Product> findByIdAndSellerProfileId(Long id, Long sellerProfileId);
}
