package com.jejulocaltime.api.service;

import com.jejulocaltime.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductExpirationServiceTest {

    @Test
    void closesExpiredActiveProducts() {
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductExpirationService service = new ProductExpirationService(productRepository);

        service.closeExpiredProducts();

        verify(productRepository).closeExpiredActiveProducts();
    }
}
