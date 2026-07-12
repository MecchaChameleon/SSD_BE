package com.jejulocaltime.api.domain.frontend;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class FrontendDto {
    private FrontendDto() {}

    public record PageResponse<T>(List<T> content, int number, int size, long totalElements, int totalPages, boolean last) {}
    public record ProductResponse(Long id, Long sellerProfileId, String name, String businessName,
            String businessType, String category, String type, Integer totalQty, Integer qty,
            Integer price, Integer minPrice, Integer currentPrice, Double discountRate,
            OffsetDateTime openTime, OffsetDateTime deadline, String address, Double lat, Double lng,
            Boolean urgent, String aiInsight, List<String> imageUrls, String status,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, Boolean wishlisted) {}
    public record PurchaseRequest(Long productId, Integer quantity) {}
    public record ReservationCreateRequest(Long productId, Integer quantity, OffsetDateTime visitStartAt, OffsetDateTime visitEndAt) {}
    public record CancelRequest(String reason) {}
    public record RejectRequest(String reasonCode, String reason) {}
    public record ReservationResponse(Long id, Long productId, String productName, String businessName,
            Long buyerId, String buyerNickname, Integer quantity, Integer unitPrice, Integer totalAmount,
            OffsetDateTime visitStartAt, OffsetDateTime visitEndAt, String status, String rejectReason,
            OffsetDateTime requestedAt) {}
    public record DashboardResponse(LocalDate date, ReservationCounts reservationCounts, long dailyRevenue,
            long periodRevenue, long registeredProductCount) {}
    public record ReservationCounts(long requested, long approved, long noShow) {}
    public record SalesItem(Long productId, String productName, long quantity, long revenue) {}
    public record SalesReportResponse(LocalDate startDate, LocalDate endDate, long totalRevenue, long totalQuantity, List<SalesItem> items) {}
    public record PriceApplyRequest(Integer price, Long recommendationId) {}
    public record UserUpdateRequest(String nickname, String profileImageUrl) {}
    public record NotificationResponse(Long id, String type, String title, String message, String referenceType,
            Long referenceId, boolean read, OffsetDateTime createdAt) {}
    public record NotificationSettings(boolean commonEvent, boolean sellerReservation, boolean sellerAiPrice,
            boolean sellerSettlement, boolean buyerDeadline, boolean buyerReservationApproved) {}
    public record PushTokenRequest(String deviceToken, String platform) {}
}
