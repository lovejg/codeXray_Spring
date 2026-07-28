package com.codeXray.backend.problem.repository;

import com.codeXray.backend.problem.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProblemRepository
        extends JpaRepository<Problem, Long>, ProblemRepositoryCustom {

    // 붙여넣은 URL 매칭용: link 가 "/lessons/{번호}" 로 끝나는 문제 조회
    // (파생 쿼리 → 자동으로 WHERE link LIKE %/lessons/{번호} 생성)
    Optional<Problem> findByLinkEndingWith(String suffix);
}
