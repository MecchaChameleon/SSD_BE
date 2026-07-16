package com.jejulocaltime.api.service;

import com.jejulocaltime.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountWithdrawalServiceTest {
    @Mock UserRepository userRepository;
    @Mock JdbcTemplate jdbc;
    private AccountWithdrawalService service;

    @BeforeEach
    void setUp() {
        service = new AccountWithdrawalService(userRepository, jdbc);
    }

    @Test
    void rejectsWithdrawalWhenARequestedPaidOrderExists() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(1L), eq(1L))).thenReturn(1L);

        assertThatThrownBy(() -> service.withdraw(1L))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                    assertThat(error.getStatusCode().value()).isEqualTo(409);
                    assertThat(error.getReason()).contains("판매자 확인 대기");
                });

        verify(userRepository, never()).deleteById(1L);
    }

    @Test
    void deletesAndFlushesUserWhenNoPendingOrderExists() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(1L), eq(1L))).thenReturn(0L);

        service.withdraw(1L);

        verify(userRepository).deleteById(1L);
        verify(userRepository).flush();
    }
}
