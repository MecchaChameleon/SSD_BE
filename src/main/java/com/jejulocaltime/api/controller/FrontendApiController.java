package com.jejulocaltime.api.controller;

import com.jejulocaltime.api.common.response.ApiResponseTemplate;
import com.jejulocaltime.api.dto.FrontendDto.*;
import com.jejulocaltime.api.dto.MeResponse;
import com.jejulocaltime.api.service.FrontendApiService;
import com.jejulocaltime.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FrontendApiController {
    private final FrontendApiService service;
    private final UserRepository users;

    @GetMapping("/api/products")
    public ResponseEntity<ApiResponseTemplate<PageResponse<ProductResponse>>> products(@AuthenticationPrincipal Long userId,
            @RequestParam(required=false)String query,@RequestParam(required=false)String businessType,
            @RequestParam(required=false)String category,@RequestParam(required=false)String sort,
            @RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return ApiResponseTemplate.success(service.products(userId,query,businessType,category,sort,page,Math.min(size,50)));}
    @GetMapping("/api/products/{id}") public ResponseEntity<ApiResponseTemplate<ProductResponse>> product(@AuthenticationPrincipal Long userId,@PathVariable Long id){return ApiResponseTemplate.success(service.product(userId,id));}
    // 지도에 판매중 상품 핀을 뿌리기 위한 API. 뷰포트 좌표(swLat/swLng/neLat/neLng)를 모두 보내면 그 범위 안의 상품만 반환한다.
    @GetMapping("/api/products/map") public ResponseEntity<ApiResponseTemplate<List<MapPinResponse>>> productsMap(
            @RequestParam(required=false)Double swLat,@RequestParam(required=false)Double swLng,
            @RequestParam(required=false)Double neLat,@RequestParam(required=false)Double neLng){
        return ApiResponseTemplate.success(service.mapPins(swLat,swLng,neLat,neLng));
    }

    @PostMapping("/api/buyer/purchases") public ResponseEntity<ApiResponseTemplate<PurchaseResponse>> purchase(@AuthenticationPrincipal Long userId,@RequestBody PurchaseRequest body){return ApiResponseTemplate.success(service.purchase(userId,body));}
    @GetMapping("/api/buyer/purchases") public ResponseEntity<ApiResponseTemplate<PageResponse<PurchaseResponse>>> buyerPurchases(@AuthenticationPrincipal Long userId,@RequestParam(required=false)String status,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return ApiResponseTemplate.success(service.buyerPurchases(userId,status,page,size));}
    @GetMapping("/api/buyer/purchases/{id}") public ResponseEntity<ApiResponseTemplate<PurchaseResponse>> buyerPurchase(@AuthenticationPrincipal Long userId,@PathVariable Long id){return ApiResponseTemplate.success(service.purchase(userId,id,false));}
    @DeleteMapping("/api/buyer/purchases/{id}/history") public ResponseEntity<ApiResponseTemplate<Void>> hidePurchase(@AuthenticationPrincipal Long userId,@PathVariable Long id){service.hidePurchase(userId,id);return ApiResponseTemplate.success();}
    @GetMapping("/api/buyer/wishlist") public ResponseEntity<ApiResponseTemplate<PageResponse<ProductResponse>>> wishlist(@AuthenticationPrincipal Long userId,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return ApiResponseTemplate.success(service.wishlist(userId,page,size));}
    @PostMapping("/api/buyer/wishlist/{productId}") public ResponseEntity<ApiResponseTemplate<Void>> addWish(@AuthenticationPrincipal Long userId,@PathVariable Long productId){service.addWish(userId,productId);return ApiResponseTemplate.success();}
    @DeleteMapping("/api/buyer/wishlist/{productId}") public ResponseEntity<ApiResponseTemplate<Void>> removeWish(@AuthenticationPrincipal Long userId,@PathVariable Long productId){service.removeWish(userId,productId);return ApiResponseTemplate.success();}

    @GetMapping("/api/seller/payments") public ResponseEntity<ApiResponseTemplate<PageResponse<PurchaseResponse>>> sellerPayments(@AuthenticationPrincipal Long userId,@RequestParam(required=false)String status,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate date,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return ApiResponseTemplate.success(service.sellerPayments(userId,status,date,page,size));}
    @PatchMapping("/api/seller/payments/{id}/accept") public ResponseEntity<ApiResponseTemplate<PurchaseResponse>> acceptPayment(@AuthenticationPrincipal Long userId,@PathVariable Long id){return ApiResponseTemplate.success(service.sellerPaymentAction(userId,id,true,null));}
    @PatchMapping("/api/seller/payments/{id}/reject") public ResponseEntity<ApiResponseTemplate<PurchaseResponse>> rejectPayment(@AuthenticationPrincipal Long userId,@PathVariable Long id,@RequestBody RejectRequest body){return ApiResponseTemplate.success(service.sellerPaymentAction(userId,id,false,body.reason()));}
    @GetMapping("/api/seller/dashboard") public ResponseEntity<ApiResponseTemplate<DashboardResponse>> dashboard(@AuthenticationPrincipal Long userId,@RequestParam@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate date){return ApiResponseTemplate.success(service.dashboard(userId,date));}
    @GetMapping("/api/seller/sales/report") public ResponseEntity<ApiResponseTemplate<SalesReportResponse>> sales(@AuthenticationPrincipal Long userId,@RequestParam@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate startDate,@RequestParam@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate endDate,@RequestParam(defaultValue="REVENUE_DESC")String sort){return ApiResponseTemplate.success(service.sales(userId,startDate,endDate,sort));}
    @GetMapping("/api/seller/sales/history") public ResponseEntity<ApiResponseTemplate<PageResponse<SalesHistoryItem>>> salesHistory(@AuthenticationPrincipal Long userId,@RequestParam@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate startDate,@RequestParam@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate endDate,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponseTemplate.success(service.salesHistory(userId,startDate,endDate,page,size));}
    @PostMapping("/api/seller/products/{id}/price/apply") public ResponseEntity<ApiResponseTemplate<ProductResponse>> applyPrice(@AuthenticationPrincipal Long userId,@PathVariable Long id,@RequestBody PriceApplyRequest body){return ApiResponseTemplate.success(service.applyPrice(userId,id,body));}

    @PatchMapping("/api/users/me") public ResponseEntity<ApiResponseTemplate<MeResponse>> updateMe(@AuthenticationPrincipal Long userId,@RequestBody UserUpdateRequest body){service.updateUser(userId,body);return ApiResponseTemplate.success(users.findById(userId).map(MeResponse::from).orElseThrow());}
    @PostMapping("/api/auth/logout") public ResponseEntity<ApiResponseTemplate<Void>> logout(){return ApiResponseTemplate.success();}

    @GetMapping("/api/notifications") public ResponseEntity<ApiResponseTemplate<PageResponse<NotificationResponse>>> notifications(@AuthenticationPrincipal Long userId,@RequestParam(defaultValue="ALL")String filter,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return ApiResponseTemplate.success(service.notifications(userId,filter,page,size));}
    @PatchMapping("/api/notifications/{id}/read") public ResponseEntity<ApiResponseTemplate<Void>> read(@AuthenticationPrincipal Long userId,@PathVariable Long id){service.readNotification(userId,id);return ApiResponseTemplate.success();}
    @PatchMapping("/api/notifications/read-all") public ResponseEntity<ApiResponseTemplate<Void>> readAll(@AuthenticationPrincipal Long userId){service.readAll(userId);return ApiResponseTemplate.success();}
    @GetMapping("/api/users/me/notification-settings") public ResponseEntity<ApiResponseTemplate<NotificationSettings>> settings(@AuthenticationPrincipal Long userId){return ApiResponseTemplate.success(service.settings(userId));}
    @PutMapping("/api/users/me/notification-settings") public ResponseEntity<ApiResponseTemplate<NotificationSettings>> settings(@AuthenticationPrincipal Long userId,@RequestBody NotificationSettings body){return ApiResponseTemplate.success(service.updateSettings(userId,body));}
    @PostMapping("/api/users/me/push-tokens") public ResponseEntity<ApiResponseTemplate<Void>> push(@AuthenticationPrincipal Long userId,@RequestBody PushTokenRequest body){service.pushToken(userId,body);return ApiResponseTemplate.success();}
    @DeleteMapping("/api/users/me/push-tokens/{token}") public ResponseEntity<ApiResponseTemplate<Void>> removePush(@AuthenticationPrincipal Long userId,@PathVariable String token){service.removePushToken(userId,token);return ApiResponseTemplate.success();}
}
