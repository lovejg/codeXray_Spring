package com.codeXray.backend.stats.dto;

import com.codeXray.backend.problem.entity.Tier;

// 집계 쿼리 결과를 담는 인터페이스 프로젝션들.
// JPQL 의 `... as tier`, `... as count` 별칭이 게터명(getTier/getCount)과 연결된다.
public final class StatsProjections {
    private StatsProjections() {}

    public interface TierCount {
        Tier getTier();
        long getCount();
    }

    public interface LangCount {
        String getLanguage();
        long getCount();
    }

    public interface TagCount {
        String getTag();
        long getCount();
    }

    public interface WeakTagCount {
        String getTag();
        double getAvgLevel();
        long getCount();
    }
}
