package com.codeXray.backend.problem.service;

import com.codeXray.backend.common.dto.PageResponse;
import com.codeXray.backend.common.exception.BusinessException;
import com.codeXray.backend.common.exception.ErrorCode;
import com.codeXray.backend.problem.dto.ProblemResponse;
import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.entity.ProblemSource;
import com.codeXray.backend.problem.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemService {
    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProblemResponse> search(String keyword, ProblemSource source,
                                                Integer tierMin, Integer tierMax, Long tagId, Pageable pageable) {
        Page<Problem> page = problemRepository.search(keyword, source, tierMin, tierMax, tagId, pageable);
        Page<ProblemResponse> dtoPage = page.map(ProblemResponse::from);
        return PageResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public ProblemResponse getById(Long id) {
        Problem problem = problemRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return ProblemResponse.from(problem);
    }
}
