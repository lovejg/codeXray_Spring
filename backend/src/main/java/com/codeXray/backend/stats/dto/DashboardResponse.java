package com.codeXray.backend.stats.dto;

import java.time.LocalDate;
import java.util.List;

// 개인 대시보드 응답. 프론트가 그대로 차트/잔디밭으로 그림.
public record DashboardResponse(
        Summary summary,
        List<TierSlice> tiers,     // 티어 분포
        List<CountSlice> languages,// 언어 분포
        List<CountSlice> topTags,  // 많이 푼 태그(강점)
        List<WeakSlice> weakTags,  // 어렵게 느낀 태그(약점)
        List<HeatCell> heatmap     // 최근 N일 일별 해결 수(잔디밭)
) {
    public record Summary(long totalSolved, long starred, long notes,
                          int currentStreak, int longestStreak) {}

    public record TierSlice(String tier, long count) {}

    public record CountSlice(String label, long count) {}

    public record WeakSlice(String tag, double avgLevel, long count) {}

    public record HeatCell(LocalDate date, long count) {}
}
