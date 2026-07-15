package com.jejulocaltime.api.service;

import com.jejulocaltime.api.dto.FrontendDto.PurchaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FrontendApiServiceTest {

    private static final long USER_ID = 1L;
    private static final long PURCHASE_ID = 10L;

    private JdbcTemplate jdbc;
    private FrontendApiService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new FrontendApiService(jdbc);
    }

    @Test
    void sellerCanAcceptPendingPaidPurchase() {
        givenPurchase(response("PENDING", "PAID"));
        when(jdbc.queryForMap(anyString(), eq(PURCHASE_ID), eq(USER_ID)))
                .thenReturn(Map.of("status", "REQUESTED", "payment_status", "PAID"));

        service.sellerPaymentAction(USER_ID, PURCHASE_ID, true, null);

        verify(jdbc).update(contains("status='COMPLETED'"), eq(PURCHASE_ID));
    }

    @Test
    void rejectingPendingPaymentRefundsAndRestoresStock() {
        givenPurchase(response("PENDING", "PAID"));
        when(jdbc.queryForMap(anyString(), eq(PURCHASE_ID), eq(USER_ID)))
                .thenReturn(Map.of("status", "REQUESTED", "payment_status", "PAID"));

        service.sellerPaymentAction(USER_ID, PURCHASE_ID, false, "재고 부족");

        verify(jdbc).update(contains("payment_status='REFUNDED'"), eq("재고 부족"), eq(PURCHASE_ID));
        verify(jdbc).update(contains("remaining_quantity=remaining_quantity+"), eq(1), eq(20L));
    }

    @Test
    void acceptedPaymentCannotBeRejectedAfterward() {
        givenPurchase(response("ACCEPTED", "PAID"));
        when(jdbc.queryForMap(anyString(), eq(PURCHASE_ID), eq(USER_ID)))
                .thenReturn(Map.of("status", "COMPLETED", "payment_status", "PAID"));

        assertThatThrownBy(() -> service.sellerPaymentAction(USER_ID, PURCHASE_ID, false, "변심"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(jdbc, never()).update(contains("payment_status='REFUNDED'"), any(), any());
    }

    @Test
    void pendingPurchaseHistoryCannotBeHidden() {
        givenPurchase(response("PENDING", "PAID"));

        assertThatThrownBy(() -> service.hidePurchase(USER_ID, PURCHASE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void acceptedPurchaseHistoryCanBeHidden() {
        givenPurchase(response("ACCEPTED", "PAID"));

        service.hidePurchase(USER_ID, PURCHASE_ID);

        verify(jdbc).update(contains("hidden_by_buyer=true"), eq(PURCHASE_ID), eq(USER_ID));
    }

    @Test
    void salesHistoryRejectsReversedDateRange() {
        assertThatThrownBy(() -> service.salesHistory(
                USER_ID, LocalDate.of(2026, 7, 15), LocalDate.of(2026, 7, 14), 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void givenPurchase(PurchaseResponse response) {
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(PURCHASE_ID), eq(USER_ID)))
                .thenReturn(response);
    }

    private PurchaseResponse response(String status, String paymentStatus) {
        return new PurchaseResponse(
                PURCHASE_ID, 20L, "상품", "매장", USER_ID, "구매자",
                1, 20_000, 10_000, 10_000, status, null, OffsetDateTime.now(), paymentStatus);
    }
}
