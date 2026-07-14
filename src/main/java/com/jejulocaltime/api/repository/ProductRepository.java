package com.jejulocaltime.api.repository;

import com.jejulocaltime.api.domain.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE product
               SET status = 'CLOSED', updated_at = now()
             WHERE status = 'ACTIVE'
               AND reservation_close_at <= now()
            """, nativeQuery = true)
    int closeExpiredActiveProducts();

    Page<Product> findBySellerProfileId(Long sellerProfileId, Pageable pageable);

    Page<Product> findBySellerProfileIdAndStatus(Long sellerProfileId, Product.Status status, Pageable pageable);

    Optional<Product> findByIdAndSellerProfileId(Long id, Long sellerProfileId);
}
