package com.jejulocaltime.api.controller;

import com.jejulocaltime.api.common.response.ApiResponseTemplate;
import com.jejulocaltime.api.domain.Product;
import com.jejulocaltime.api.dto.ProductDto;
import com.jejulocaltime.api.dto.ProductImageDto;
import com.jejulocaltime.api.dto.ProductPriceDto;
import com.jejulocaltime.api.dto.ProductStrategyDto;
import com.jejulocaltime.api.service.ProductImageService;
import com.jejulocaltime.api.service.ProductPricingService;
import com.jejulocaltime.api.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Seller-Product", description = "판매자 상품/자원 관리 API (SEL-01~04)")
@RestController
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
public class ProductController {

    private static final int PAGE_SIZE = 10;

    private final ProductService productService;
    private final ProductImageService productImageService;
    private final ProductPricingService productPricingService;

    // 1. 상품/자원 등록
    @Operation(summary = "상품/자원 등록", description = "로그인한 판매자 본인 명의로 상품/자원을 등록한다.")
    @PostMapping
    public ResponseEntity<ApiResponseTemplate<ProductDto.Response>> createProduct(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProductDto.CreateRequest request) {

        ProductDto.Response response = productService.createProduct(userId, request);
        return ApiResponseTemplate.success(HttpStatus.CREATED, response);
    }

    // 2. 상품/자원 수정
    @Operation(summary = "상품/자원 수정", description = "본인이 등록한 상품/자원의 일부 필드를 수정한다.")
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponseTemplate<ProductDto.Response>> updateProduct(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @RequestBody ProductDto.UpdateRequest request) {

        ProductDto.Response response = productService.updateProduct(userId, productId, request);
        return ApiResponseTemplate.success(response);
    }

    // 3. AI 추천 할인가 조회
    @Operation(summary = "AI 추천 할인가 조회", description = "AI 서버가 계산한 추천 할인가를 그대로 전달한다.")
    @GetMapping("/{productId}/price")
    public ResponseEntity<ApiResponseTemplate<ProductPriceDto.Response>> getPriceRecommendation(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId) {

        ProductPriceDto.Response response = productPricingService.getPriceRecommendation(userId, productId);
        return ApiResponseTemplate.success(response);
    }

    // 4. 판매 전략 제안 조회
    @Operation(summary = "판매 전략 제안 조회", description = "AI 서버가 생성한 판매 전략 문구를 그대로 전달한다.")
    @GetMapping("/{productId}/strategy")
    public ResponseEntity<ApiResponseTemplate<ProductStrategyDto.Response>> getStrategy(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId) {

        ProductStrategyDto.Response response = productPricingService.getStrategy(userId, productId);
        return ApiResponseTemplate.success(response);
    }

    // 5. 내 상품/자원 목록 조회
    @Operation(summary = "내 상품/자원 목록 조회", description = "로그인한 판매자 본인의 상품/자원을 페이징 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponseTemplate<Page<ProductDto.Response>>> getMyProducts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Product.Status status,
            @RequestParam(defaultValue = "0") int page) {

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProductDto.Response> response = productService.getMyProducts(userId, status, pageable);
        return ApiResponseTemplate.success(response);
    }

    // 6. 내 상품/자원 단건 조회
    @Operation(summary = "내 상품/자원 단건 조회", description = "본인이 등록한 상품/자원 하나를 조회한다.")
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponseTemplate<ProductDto.Response>> getMyProduct(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId) {

        ProductDto.Response response = productService.getMyProduct(userId, productId);
        return ApiResponseTemplate.success(response);
    }

    // 7. 상품/자원 삭제
    @Operation(summary = "상품/자원 삭제", description = "진행 중인 예약이 없는 경우에만 본인 상품/자원을 삭제한다.")
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponseTemplate<Void>> deleteProduct(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId) {

        productService.deleteProduct(userId, productId);
        return ApiResponseTemplate.success();
    }

    // 8. 상품 이미지 업로드
    @Operation(summary = "상품 이미지 업로드", description = "본인 상품에 이미지를 업로드하고 업로드된 이미지 URL 목록을 반환한다.")
    @PostMapping("/{productId}/images")
    public ResponseEntity<ApiResponseTemplate<ProductImageDto.UploadResponse>> uploadImages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @RequestPart("images") List<MultipartFile> images) {

        ProductImageDto.UploadResponse response = productImageService.uploadImages(userId, productId, images);
        return ApiResponseTemplate.success(HttpStatus.CREATED, response);
    }

    // 9. 판매상태 변경
    @Operation(summary = "판매상태 변경", description = "본인 상품/자원의 판매상태(ACTIVE/PAUSED/CLOSED)를 변경한다.")
    @PatchMapping("/{productId}/status")
    public ResponseEntity<ApiResponseTemplate<ProductDto.Response>> updateStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long productId,
            @Valid @RequestBody ProductDto.StatusUpdateRequest request) {

        ProductDto.Response response = productService.updateStatus(userId, productId, request.status());
        return ApiResponseTemplate.success(response);
    }
}
