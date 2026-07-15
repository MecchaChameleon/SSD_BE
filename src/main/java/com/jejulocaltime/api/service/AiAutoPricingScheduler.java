package com.jejulocaltime.api.service;

import com.jejulocaltime.api.domain.Product;
import com.jejulocaltime.api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAutoPricingScheduler {
    private final ProductRepository productRepository;
    private final ProductPricingService productPricingService;

    @Scheduled(fixedDelayString = "${ai.pricing.interval-ms:600000}")
    public void updateActiveProducts() {
        for (Product product : productRepository.findByStatusAndAiAutoPricingEnabledTrue(Product.Status.ACTIVE)) {
            try {
                productPricingService.recalculateAndApply(product.getId());
            } catch (RuntimeException exception) {
                log.warn("AI auto pricing failed for product {}: {}", product.getId(), exception.getMessage());
            }
        }
    }
}
