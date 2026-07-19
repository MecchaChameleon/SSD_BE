package com.jejulocaltime.api.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DummyProductSeederTest {

    @Test
    void schedulesTwentyProductsHourlyInKoreaTime() {
        LocalDate date = LocalDate.of(2026, 7, 20);

        var first = DummyProductSeeder.salesWindow(date, 0);
        var last = DummyProductSeeder.salesWindow(date, 19);

        assertThat(first.startAt()).isEqualTo(date.atTime(1, 0).atOffset(ZoneOffset.ofHours(9)));
        assertThat(last.startAt()).isEqualTo(date.atTime(20, 0).atOffset(ZoneOffset.ofHours(9)));
        assertThat(last.closeAt()).isEqualTo(date.plusDays(1).atTime(19, 55).atOffset(ZoneOffset.ofHours(9)));
        assertThat(Duration.between(first.startAt(), first.closeAt()))
                .isEqualTo(Duration.ofHours(23).plusMinutes(55));
    }
}
