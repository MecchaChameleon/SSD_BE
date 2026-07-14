package com.jejulocaltime.api.service;

import com.jejulocaltime.api.common.storage.FileStorage;
import com.jejulocaltime.api.domain.Product;
import com.jejulocaltime.api.domain.ProductImage;
import com.jejulocaltime.api.dto.ProductImageDto;
import com.jejulocaltime.api.repository.ProductImageRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

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
        if (images == null || images.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "이미지를 1장 이상 등록해 주세요.");
        if (nextSortOrder + images.size() > 3) throw new ResponseStatusException(BAD_REQUEST, "상품 이미지는 최대 3장까지 등록할 수 있습니다.");
        for (MultipartFile image : images) {
            if (image.isEmpty() || image.getContentType() == null || !image.getContentType().startsWith("image/"))
                throw new ResponseStatusException(BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다.");
            if (image.getSize() > 10 * 1024 * 1024)
                throw new ResponseStatusException(BAD_REQUEST, "이미지 한 장의 크기는 10MB 이하여야 합니다.");
        }
        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile image : images) {
            String imageUrl = fileStorage.store(image, "products/" + product.getId());
            productImageRepository.save(new ProductImage(product.getId(), imageUrl, nextSortOrder++));
            uploadedUrls.add(imageUrl);
        }

        return new ProductImageDto.UploadResponse(uploadedUrls);
    }
}
