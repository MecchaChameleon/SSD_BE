package com.jejulocaltime.api.service;

import com.jejulocaltime.api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists the terminal state of products whose sales deadline has passed. */
@Service
@RequiredArgsConstructor
public class ProductExpirationService {

    private final ProductRepository productRepository;

    @Scheduled(fixedDelayString = "${product.expiration-check-interval-ms:5000}")
    @Transactional
    public void closeExpiredProducts() {
        productRepository.closeExpiredActiveProducts();
    }
}
