package com.codeXray.backend.problem.service;

import com.codeXray.backend.config.CacheConfig;
import com.codeXray.backend.problem.dto.TagListResponse;
import com.codeXray.backend.problem.dto.TagResponse;
import com.codeXray.backend.problem.repository.AlgorithmTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final AlgorithmTagRepository tagRepository;

    /*
     * 태그 목록(25개 남짓)은 런타임에 거의 바뀌지 않는 전형적인 참조 데이터.
     * 인자가 없어 캐시는 사실상 단일 엔트리 → TTL-only 전략(별도 evict 없이 1시간 뒤 자동 갱신).
     */
    @Cacheable(cacheNames = CacheConfig.TAG_LIST)
    @Transactional(readOnly = true)
    public TagListResponse getAllTags() {
        List<TagResponse> tags = tagRepository.findAll(Sort.by("name")).stream()
                .map(TagResponse::from)
                .toList();
        return TagListResponse.of(tags);
    }
}
