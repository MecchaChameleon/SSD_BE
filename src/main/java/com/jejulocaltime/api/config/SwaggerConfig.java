package com.jejulocaltime.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jeju Localtime API")
                        .description("제주 로컬타임 API 문서")
                        .version("v0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    /**
     * 문서화 어노테이션이 빠진 엔드포인트도 Swagger UI에서 빈 설명으로 노출되지 않게 한다.
     * 컨트롤러의 @Operation 값이 있으면 그 값을 보존하고, 공통 HTTP 오류 의미를 모든 API에 표시한다.
     */
    @Bean
    public OpenApiCustomizer completeOperationDocumentation() {
        Map<String, String> summaries = Map.ofEntries(
                Map.entry("/api/products", "판매 상품 목록 조회"),
                Map.entry("/api/products/{id}", "판매 상품 상세 조회"),
                Map.entry("/api/buyer/purchases", "상품 즉시 구매"),
                Map.entry("/api/buyer/reservations", "구매자 예약 생성·목록 조회"),
                Map.entry("/api/buyer/reservations/{id}", "구매자 예약 상세 조회"),
                Map.entry("/api/buyer/reservations/{id}/cancel", "구매자 예약 취소"),
                Map.entry("/api/buyer/reservations/{id}/history", "구매자 예약 내역 숨김"),
                Map.entry("/api/buyer/wishlist", "찜 상품 목록 조회"),
                Map.entry("/api/buyer/wishlist/{productId}", "상품 찜 추가·해제"),
                Map.entry("/api/seller/reservations", "판매자 예약 목록 조회"),
                Map.entry("/api/seller/reservations/{id}/approve", "예약 승인"),
                Map.entry("/api/seller/reservations/{id}/reject", "예약 거절"),
                Map.entry("/api/seller/reservations/{id}/complete", "방문·판매 완료 처리"),
                Map.entry("/api/seller/reservations/{id}/no-show", "노쇼 처리"),
                Map.entry("/api/seller/dashboard", "판매자 대시보드 조회"),
                Map.entry("/api/seller/sales/report", "판매 매출 리포트 조회"),
                Map.entry("/api/seller/products/{id}/price/apply", "추천 가격 적용"),
                Map.entry("/api/users/me", "내 회원 정보 수정"),
                Map.entry("/api/auth/logout", "로그아웃"),
                Map.entry("/api/notifications", "알림 목록 조회"),
                Map.entry("/api/notifications/{id}/read", "알림 읽음 처리"),
                Map.entry("/api/notifications/read-all", "모든 알림 읽음 처리"),
                Map.entry("/api/users/me/notification-settings", "알림 설정 조회·수정"),
                Map.entry("/api/users/me/push-tokens", "푸시 토큰 등록"),
                Map.entry("/api/users/me/push-tokens/{token}", "푸시 토큰 삭제")
        );
        return openApi -> {
            openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    String summary = summaries.getOrDefault(path, method + " " + path);
                    if (operation.getSummary() == null || operation.getSummary().isBlank()) {
                        operation.setSummary(summary);
                    }
                    if (operation.getDescription() == null || operation.getDescription().isBlank()) {
                        operation.setDescription(summary + " API입니다. 인증이 필요한 API는 Authorization 헤더에 Bearer JWT를 전달해야 합니다.");
                    }
                    operation.getResponses().putIfAbsent("400", new ApiResponse().description("요청값 형식 또는 유효성 검증 실패"));
                    operation.getResponses().putIfAbsent("401", new ApiResponse().description("인증 토큰이 없거나 유효하지 않음"));
                    operation.getResponses().putIfAbsent("403", new ApiResponse().description("해당 작업을 수행할 권한이 없음"));
                    operation.getResponses().putIfAbsent("404", new ApiResponse().description("요청한 사용자 또는 리소스를 찾을 수 없음"));
                    operation.getResponses().putIfAbsent("409", new ApiResponse().description("현재 리소스 상태와 요청 작업이 충돌함"));
                    operation.getResponses().putIfAbsent("500", new ApiResponse().description("서버 내부 오류"));
                }));
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().forEach((name, schema) -> {
                    if (schema.getDescription() == null) schema.setDescription(name + " 요청·응답 데이터 모델");
                    if (schema.getProperties() != null) schema.getProperties().forEach((field, value) -> {
                        Schema<?> property = (Schema<?>) value;
                        if (property.getDescription() == null) property.setDescription(field + " 값");
                    });
                });
            }
        };
    }
}
