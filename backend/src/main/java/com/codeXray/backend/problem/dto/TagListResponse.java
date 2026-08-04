package com.codeXray.backend.problem.dto;

import java.util.List;

/*
 * 태그 목록 응답 엔벨로프.
 * bare 배열([...]) 대신 객체로 감싼다 — Redis 캐시(GenericJackson2Json)는 최상위가 컬렉션이면
 * 타입 정보를 일관되게 싣지 못해 역직렬화가 깨진다. record로 감싸면 @class가 실려 안전하게 왕복된다.
 * (PageResponse 와 동일한 이유·패턴.)
 */
public record TagListResponse(List<TagResponse> tags) {
    public static TagListResponse of(List<TagResponse> tags) {
        return new TagListResponse(tags);
    }
}
