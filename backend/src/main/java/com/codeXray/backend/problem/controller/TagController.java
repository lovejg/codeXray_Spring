package com.codeXray.backend.problem.controller;

import com.codeXray.backend.problem.dto.TagListResponse;
import com.codeXray.backend.problem.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    // GET /api/tags → {"tags":[...]} 전체 알고리즘 태그 목록(문제 필터 UI용). 공개.
    @GetMapping
    public TagListResponse list() {
        return tagService.getAllTags();
    }
}
