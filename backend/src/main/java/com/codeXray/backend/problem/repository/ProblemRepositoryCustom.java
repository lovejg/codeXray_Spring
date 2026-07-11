package com.codeXray.backend.problem.repository;

import com.codeXray.backend.problem.entity.Problem;
import com.codeXray.backend.problem.entity.ProblemSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


// QueryDSL 커스텀 인터페이스
public interface ProblemRepositoryCustom {

    Page<Problem> search(String keyword,
                         ProblemSource source,
                         Integer tierMin,
                         Integer tierMax,
                         Long tagId,
                         Pageable pageable);
}
