package com.codeXray.backend.stats.service;

import com.codeXray.backend.note.repository.NoteRepository;
import com.codeXray.backend.rating.repository.LevelFeedbackRepository;
import com.codeXray.backend.solution.repository.SolutionRepository;
import com.codeXray.backend.stats.dto.DashboardResponse;
import com.codeXray.backend.stats.dto.DashboardResponse.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private static final int HEATMAP_DAYS = 180; // 잔디밭 범위(약 6개월)
    private static final int TOP_TAGS = 8;
    private static final int WEAK_TAGS = 5;
    private static final int HARD_LEVEL = 3;      // 이 이상이면 "어렵게 느낌"(약점 기준)

    private final SolutionRepository solutionRepository;
    private final LevelFeedbackRepository levelFeedbackRepository;
    private final NoteRepository noteRepository;

    public DashboardResponse getDashboard(Long userId) {
        long totalSolved = solutionRepository.countByUserId(userId);
        long starred = solutionRepository.countByUserIdAndStarred(userId, true);
        long notes = noteRepository.countByUserId(userId);

        List<TierSlice> tiers = solutionRepository.tierDistribution(userId).stream()
                .map(t -> new TierSlice(t.getTier() == null ? "UNRATED" : t.getTier().name(), t.getCount()))
                .toList();

        List<CountSlice> languages = solutionRepository.languageDistribution(userId).stream()
                .map(l -> new CountSlice(l.getLanguage(), l.getCount()))
                .toList();

        List<CountSlice> topTags = solutionRepository.topTags(userId, PageRequest.of(0, TOP_TAGS)).stream()
                .map(t -> new CountSlice(t.getTag(), t.getCount()))
                .toList();

        List<WeakSlice> weakTags = levelFeedbackRepository.weakTags(userId, PageRequest.of(0, WEAK_TAGS)).stream()
                .map(w -> new WeakSlice(w.getTag(), round1(w.getAvgLevel()), w.getCount()))
                .toList();

        // 날짜 기반 지표(잔디밭/스트릭)는 생성 시각을 받아 Java 에서 계산
        List<LocalDateTime> createdAts = solutionRepository.findCreatedAtsByUserId(userId);
        List<HeatCell> heatmap = buildHeatmap(createdAts);
        int[] streak = computeStreaks(createdAts); // [current, longest]

        Summary summary = new Summary(totalSolved, starred, notes, streak[0], streak[1]);
        return new DashboardResponse(summary, tiers, languages, topTags, weakTags, heatmap);
    }

    // 최근 HEATMAP_DAYS 일의 일별 해결 수(0인 날은 제외 — 프론트가 빈 칸을 채움)
    private List<HeatCell> buildHeatmap(List<LocalDateTime> createdAts) {
        LocalDate from = LocalDate.now().minusDays(HEATMAP_DAYS - 1L);
        Map<LocalDate, Long> byDay = createdAts.stream()
                .map(LocalDateTime::toLocalDate)
                .filter(d -> !d.isBefore(from))
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));
        return byDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new HeatCell(e.getKey(), e.getValue()))
                .toList();
    }

    // 연속 풀이일 계산. 반환 [현재 스트릭, 최장 스트릭].
    private int[] computeStreaks(List<LocalDateTime> createdAts) {
        Set<LocalDate> days = createdAts.stream()
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.toCollection(TreeSet::new)); // 정렬+중복 제거
        if (days.isEmpty()) return new int[]{0, 0};

        // 최장: 오름차순으로 훑으며 하루씩 이어지면 run 증가
        int longest = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate d : days) {
            run = (prev != null && d.equals(prev.plusDays(1))) ? run + 1 : 1;
            longest = Math.max(longest, run);
            prev = d;
        }

        // 현재: 오늘(또는 어제)부터 하루씩 거슬러 올라가며 카운트
        LocalDate today = LocalDate.now();
        LocalDate cursor;
        if (days.contains(today)) cursor = today;
        else if (days.contains(today.minusDays(1))) cursor = today.minusDays(1);
        else return new int[]{0, longest};

        int current = 0;
        while (days.contains(cursor)) {
            current++;
            cursor = cursor.minusDays(1);
        }
        return new int[]{current, longest};
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
