package com.codeXray.backend.ai.repository;

import com.codeXray.backend.ai.entity.AiJob;
import com.codeXray.backend.ai.entity.AiJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AiJobRepository extends JpaRepository<AiJob, Long> {
    // 소유자 확인용(남의 잡 조회 방지)
    Optional<AiJob> findByIdAndUserId(Long id, Long userId);

    /*
     * 상태를 from → to 로 바꾸되, 지금 상태가 from 일 때만 바꾼다. 바뀐 행 수(0 또는 1)를 돌려준다.
     * "읽고 → 확인하고 → 쓰기"를 UPDATE 한 문장으로 합쳐 DB 가 원자적으로 판정한다.
     * Kafka 가 같은 메시지를 두 번 주거나 컨슈머가 둘이어도 1 을 받는 쪽은 하나뿐이다(유료 Claude 호출 중복 방지)
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AiJob j set j.status = :to, j.updatedAt = :now where j.id = :id and j.status = :from")
    int transition(@Param("id") Long id,
                   @Param("from") AiJobStatus from,
                   @Param("to") AiJobStatus to,
                   @Param("now") LocalDateTime now);

    // 지금 상태가 from 일 때만 FAILED 로 끝낸다(발행 실패, 처리 중단 정리용)
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AiJob j set j.status = com.codeXray.backend.ai.entity.AiJobStatus.FAILED, " +
            "j.errorCode = :errorCode, j.updatedAt = :now where j.id = :id and j.status = :from")
    int failIf(@Param("id") Long id,
               @Param("from") AiJobStatus from,
               @Param("errorCode") String errorCode,
               @Param("now") LocalDateTime now);

    // before 보다 오래 상태가 그대로인 잡을 FAILED 로 끝낸다. 컨슈머가 처리 중 죽었거나 메시지가 사라진 잡이
    // 영원히 PENDING/PROCESSING 으로 남아 화면이 끝없이 폴링하지 않게 한다
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AiJob j set j.status = com.codeXray.backend.ai.entity.AiJobStatus.FAILED, " +
            "j.errorCode = :errorCode, j.updatedAt = :now where j.status = :status and j.updatedAt < :before")
    int failStale(@Param("status") AiJobStatus status,
                  @Param("before") LocalDateTime before,
                  @Param("errorCode") String errorCode,
                  @Param("now") LocalDateTime now);
}
