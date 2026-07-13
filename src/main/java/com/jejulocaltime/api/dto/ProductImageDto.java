package com.jejulocaltime.api.dto;

import java.util.List;

public class ProductImageDto {

    // [POST] 이미지 업로드 응답
    public record UploadResponse(
            List<String> imageUrls
    ) {}
}
