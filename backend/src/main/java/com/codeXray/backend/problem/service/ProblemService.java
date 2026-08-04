package com.codeXray.backend.problem.service;

import com.codeXray.backend.common.dto.PageResponse;
import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.config.CacheConfig;
import com.codeXray.backend.problem.dto.ProblemResponse;
import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.entity.ProblemSource;
import com.codeXray.backend.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProblemService {
    private final ProblemRepository problemRepository;

    // 프로그래머스 문제 URL 안의 lesson 번호 추출용 (.../lessons/468381)
    private static final Pattern LESSON_ID = Pattern.compile("lessons/(\\d+)");

    /*
     * 조건부 캐싱: keyword(자유 검색)가 있는 요청은 캐싱하지 않는다.
     * 검색어는 사실상 무한한 조합이라 캐시 히트율은 낮고 Redis 메모리만 잠식하기 때문.
     * → browse/필터 조회(반복이 잦고 키가 유한)만 캐싱한다.
     * 키: 모든 필터 + 페이지 정보 조합(이 조합이 곧 결과를 결정하므로 그대로 키가 된다).
     */
    @Cacheable(
            cacheNames = CacheConfig.PROBLEM_LIST,
            key = "#keyword + ':' + #source + ':' + #tierMin + ':' + #tierMax + ':' + #tagId + ':' "
                + "+ #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort",
            condition = "#keyword == null || #keyword.isBlank()"
    )
    @Transactional(readOnly = true)
    public PageResponse<ProblemResponse> search(String keyword, ProblemSource source,
                                                Integer tierMin, Integer tierMax, Long tagId, Pageable pageable) {
        Page<Problem> page = problemRepository.search(keyword, source, tierMin, tierMax, tagId, pageable);
        Page<ProblemResponse> dtoPage = page.map(ProblemResponse::from);
        return PageResponse.from(dtoPage);
    }

    @Cacheable(cacheNames = CacheConfig.PROBLEM_DETAIL, key = "#id")
    @Transactional(readOnly = true)
    public ProblemResponse getById(Long id) {
        Problem problem = problemRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return ProblemResponse.from(problem);
    }

    // 붙여넣은 프로그래머스 URL로 문제를 찾는다. lesson 번호를 뽑아 link 끝과 매칭.
    @Transactional(readOnly = true)
    public ProblemResponse lookupByUrl(String url) {
        Matcher matcher = (url == null) ? null : LESSON_ID.matcher(url);
        if (matcher == null || !matcher.find()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        String lessonId = matcher.group(1);
        Problem problem = problemRepository.findByLinkEndingWith("/lessons/" + lessonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return ProblemResponse.from(problem);
    }
}
