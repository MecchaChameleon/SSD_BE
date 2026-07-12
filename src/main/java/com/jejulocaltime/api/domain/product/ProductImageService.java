package com.jejulocaltime.api.domain.product;

import com.jejulocaltime.api.common.storage.FileStorage;
import com.jejulocaltime.api.domain.product.dto.ProductImageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductImageService {

    private final ProductAccessGuard accessGuard;
    private final ProductImageRepository productImageRepository;
    private final FileStorage fileStorage;

    // 8. 상품 이미지 업로드
    @Transactional
    public ProductImageDto.UploadResponse uploadImages(Long userId, Long productId, List<MultipartFile> images) {
        Product product = accessGuard.requireOwnedProduct(userId, productId);

        int nextSortOrder = productImageRepository.countByProductId(product.getId());
        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile image : images) {
            String imageUrl = fileStorage.store(image, "products/" + product.getId());
            productImageRepository.save(new ProductImage(product.getId(), imageUrl, nextSortOrder++));
            uploadedUrls.add(imageUrl);
        }

        return new ProductImageDto.UploadResponse(uploadedUrls);
    }
}
