package com.codeXray.backend.solution.repository;

import com.codeXray.backend.solution.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    Optional<Memo> findBySolutionId(Long solutionId);
}
