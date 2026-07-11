package com.codeXray.backend.problem.repository;

import com.codeXray.backend.problem.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository
        extends JpaRepository<Problem, Long>, ProblemRepositoryCustom {
}
