package com.codeXray.backend.common.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/*
 * 목록 API 의 page/size 파라미터를 안전한 값으로 맞춘다.
 * 예전에는 풀이·노트·커뮤니티 목록에 페이지가 없어 전부를 한 번에 돌려줬다. 커뮤니티 목록은 로그인 없이 열려 있어서
 * 글이 쌓이면 요청 한 번이 테이블 전체를 읽는 구조였다. size 를 크게 줘서 같은 일을 하지 못하게 상한을 둔다
 */
public final class PageRequests {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    private PageRequests() {}

    // 정렬은 각 쿼리가 직접 정한다(목록마다 기준과 tie-breaker 가 다르다)
    public static Pageable of(Integer page, Integer size) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p, s);
    }
}
