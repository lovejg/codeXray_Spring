package com.codeXray.backend.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// 페이지네이션 응답 공통 포맷. Spring의 Page를 그대로 노출하지 않고
// {items, page, size, total, totalPages} 형태로 안정적으로 내보낸다.
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long total,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
