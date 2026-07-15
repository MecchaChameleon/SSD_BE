package com.jejulocaltime.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class FrontendDto {
    private FrontendDto() {}

    public record PageResponse<T>(List<T> content, int number, int size, long totalElements, int totalPages, boolean last) {}
    public record ProductResponse(Long id, Long sellerProfileId, String name, String description, String businessName,
            String businessType, String category, String type, Integer totalQty, Integer qty,
            Integer price, Integer minPrice, Integer currentPrice, Double discountRate,
            OffsetDateTime openTime, OffsetDateTime deadline, String address, Double lat, Double lng,
            Boolean urgent, String aiInsight, List<String> imageUrls, String status,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, Boolean wishlisted) {}
    // 지도 핀 표시용 경량 응답. 위경도가 등록된 판매중 상품만 대상으로 한다.
    public record MapPinResponse(Long id, String name, String businessName, String category,
            Integer price, Integer currentPrice, Double discountRate,
            Double lat, Double lng, String address, OffsetDateTime deadline, Boolean urgent) {}
    public record PurchaseRequest(Long productId, Integer quantity) {}
    public record RejectRequest(String reasonCode, String reason) {}
    public record PurchaseResponse(Long id, Long productId, String productName, String businessName,
            Long buyerId, String buyerNickname, Integer quantity, Integer originalPrice, Integer unitPrice, Integer totalAmount,
            String status, String rejectReason,
            OffsetDateTime requestedAt, String paymentStatus) {}
    public record DashboardResponse(LocalDate date, PaymentCounts paymentCounts, long dailyRevenue,
            long periodRevenue, long registeredProductCount) {}
    public record PaymentCounts(long pending, long accepted, long refunded) {}
    public record SalesItem(Long productId, String productName, long quantity, long revenue) {}
    public record SalesReportResponse(LocalDate startDate, LocalDate endDate, long totalRevenue,
            long settlementRevenue, long totalQuantity, List<SalesItem> items,
            String bankName, String accountNumber) {}
    public record SettlementRequest(LocalDate startDate, LocalDate endDate) {}
    public record SettlementResponse(Long id, long grossAmount, long platformFee, long paymentFee,
            long settlementAmount, String status, OffsetDateTime requestedAt) {}
    public record SalesHistoryItem(Long purchaseId, Long productId, String productName,
            Long buyerId, String buyerNickname, Integer quantity, Integer unitPrice,
            Integer totalAmount, OffsetDateTime soldAt) {}
    public record PriceApplyRequest(Integer price, Long recommendationId) {}
    public record UserUpdateRequest(String nickname, String profileImageUrl) {}
    public record ProfileImageResponse(String profileImageUrl) {}
    public record NotificationResponse(Long id, String type, String title, String message, String referenceType,
            Long referenceId, boolean read, OffsetDateTime createdAt) {}
    public record NotificationSettings(boolean commonEvent, boolean sellerPayment, boolean sellerAiPrice,
            boolean sellerSettlement, boolean buyerDeadline, boolean buyerPaymentResult) {}
    public record PushTokenRequest(String deviceToken, String platform) {}
}
