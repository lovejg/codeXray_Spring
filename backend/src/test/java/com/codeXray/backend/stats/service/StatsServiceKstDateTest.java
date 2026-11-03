package com.codeXray.backend.stats.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

// 운영 컨테이너처럼 서버 기본 시간대가 UTC 일 때, 풀이 시각이 한국 날짜로 잘리는지 본다
class StatsServiceKstDateTest {

    private TimeZone original;

    @BeforeEach
    void setUp() {
        original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(original);
    }

    @Test
    void 한국시간_오전에_푼_풀이는_그날로_센다() {
        // UTC 11/2 23:30 = KST 11/3 08:30. 예전에는 11/2 칸에 찍혔다
        assertThat(StatsService.kstDate(LocalDateTime.of(2026, 11, 2, 23, 30)))
                .isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    void 한국시간_밤에_푼_풀이는_날짜가_그대로다() {
        // UTC 11/2 14:00 = KST 11/2 23:00
        assertThat(StatsService.kstDate(LocalDateTime.of(2026, 11, 2, 14, 0)))
                .isEqualTo(LocalDate.of(2026, 11, 2));
    }
}
