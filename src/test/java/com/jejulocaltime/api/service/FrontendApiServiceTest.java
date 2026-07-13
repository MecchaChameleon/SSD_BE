package com.jejulocaltime.api.service;

import com.jejulocaltime.api.dto.FrontendDto.ReservationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

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
    private static final long RESERVATION_ID = 10L;

    private JdbcTemplate jdbc;
    private FrontendApiService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new FrontendApiService(jdbc);
    }

    @Test
    void approvedReservationCannotBeCanceled() {
        givenReservation(response("APPROVED", "UNPAID"));

        assertThatThrownBy(() -> service.cancel(USER_ID, RESERVATION_ID, "사용자 취소"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(jdbc, never()).update(contains("status='CANCELED'"), any(), any());
    }

    @Test
    void rejectedReservationHistoryCanBeHidden() {
        givenReservation(response("REJECTED", "UNPAID"));

        service.hide(USER_ID, RESERVATION_ID);

        verify(jdbc).update(contains("hidden_by_buyer=true"), eq(RESERVATION_ID), eq(USER_ID));
    }

    @Test
    void paidReservationHistoryCanBeHidden() {
        givenReservation(response("COMPLETED", "PAID"));

        service.hide(USER_ID, RESERVATION_ID);

        verify(jdbc).update(contains("hidden_by_buyer=true"), eq(RESERVATION_ID), eq(USER_ID));
    }

    @Test
    void completedButUnpaidReservationHistoryCannotBeHidden() {
        givenReservation(response("COMPLETED", "UNPAID"));

        assertThatThrownBy(() -> service.hide(USER_ID, RESERVATION_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(jdbc, never()).update(contains("hidden_by_buyer=true"), any(), any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void givenReservation(ReservationResponse response) {
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(RESERVATION_ID), eq(USER_ID)))
                .thenReturn(response);
    }

    private ReservationResponse response(String status, String paymentStatus) {
        return new ReservationResponse(
                RESERVATION_ID, 20L, "상품", "매장", USER_ID, "구매자",
                1, 10_000, 10_000, null, null, status, null,
                OffsetDateTime.now(), paymentStatus);
    }
}
