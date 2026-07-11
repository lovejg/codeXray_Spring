package com.codeXray.backend.problem.controller;

import com.codeXray.backend.common.dto.PageResponse;
import com.codeXray.backend.problem.dto.ProblemResponse;
import com.codeXray.backend.problem.entity.ProblemSource;
import com.codeXray.backend.problem.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    // GET /api/problems?keyword=..&source=..&tierMin=..&tierMax=..&tagId=..&page=0&size=50&sort=createdAt,desc
    @GetMapping
    public PageResponse<ProblemResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProblemSource source,
            @RequestParam(required = false) Integer tierMin,
            @RequestParam(required = false) Integer tierMax,
            @RequestParam(required = false) Long tagId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return problemService.search(keyword, source, tierMin, tierMax, tagId, pageable);
    }

    // GET /api/problems/{id}
    @GetMapping("/{id}")
    public ProblemResponse detail(@PathVariable Long id) {
        return problemService.getById(id);
    }
}
