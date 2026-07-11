package com.codeXray.backend.problem.repository;

import com.codeXray.backend.problem.entity.AlgorithmTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlgorithmTagRepository extends JpaRepository<AlgorithmTag, Long> {
}
